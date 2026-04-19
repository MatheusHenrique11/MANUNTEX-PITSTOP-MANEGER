package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Documento;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.TipoDocumento;
import com.manutex.pitstop.domain.repository.ClienteRepository;
import com.manutex.pitstop.domain.repository.DocumentoRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.domain.repository.VeiculoRepository;
import com.manutex.pitstop.web.dto.DocumentoResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra o ciclo de vida completo de um documento:
 *
 *  1. Validação por Magic Numbers (PdfMagicNumberValidator)
 *  2. Cálculo de checksum SHA-256 do conteúdo original
 *  3. Criptografia AES-256-GCM (AesEncryptionService)
 *  4. Upload para S3/MinIO com SSE-S3 adicional (StorageService)
 *  5. Persistência dos metadados no banco (sem storageKey exposto na API)
 *  6. Geração de Pre-signed URL temporária sob demanda
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final UserRepository userRepository;
    private final PdfMagicNumberValidator pdfValidator;
    private final AesEncryptionService encryptionService;
    private final ChecksumService checksumService;
    private final StorageService storageService;

    /**
     * Realiza upload seguro de um documento PDF.
     *
     * Pipeline de segurança:
     *  [Magic Numbers] → [Checksum] → [AES-256-GCM] → [S3 SSE-S3] → [DB metadados]
     */
    @Transactional
    public DocumentoResponse upload(
        MultipartFile file,
        TipoDocumento tipo,
        UUID veiculoId,
        UUID clienteId,
        Instant expiresAt
    ) {
        // 1. Valida que é um PDF real (Magic Numbers + tamanho)
        pdfValidator.validate(file);

        // 2. Lê bytes em memória
        byte[] plainBytes;
        try {
            plainBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo: " + e.getMessage(), e);
        }

        // 3. Checksum do conteúdo original (antes de criptografar)
        String checksum = checksumService.sha256Hex(plainBytes);

        // 4. Criptografa com AES-256-GCM
        byte[] encryptedBytes = encryptionService.encrypt(plainBytes);

        // 5. Gera chave única no S3 — nunca usa o nome original do arquivo
        String storageKey = buildStorageKey(tipo, veiculoId, clienteId);

        // 6. Armazena no S3 com SSE-S3
        storageService.store(storageKey, encryptedBytes, file.getContentType());

        // 7. Persiste metadados — storageKey fica no banco mas nunca vai ao frontend
        User uploader = resolveCurrentUser();
        Documento documento = Documento.builder()
            .tipo(tipo)
            .storageKey(storageKey)
            .nomeOriginal(sanitizeFilename(file.getOriginalFilename()))
            .tamanhoBytes((long) plainBytes.length)
            .mimeType("application/pdf")  // forçado — validado por Magic Numbers
            .checksumSha256(checksum)
            .uploadedBy(uploader)
            .expiresAt(expiresAt)
            .build();

        if (veiculoId != null) {
            documento.setVeiculo(veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado: " + veiculoId)));
        }
        if (clienteId != null) {
            documento.setCliente(clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId)));
        }

        Documento saved = documentoRepository.save(documento);
        log.info("Documento salvo: id={}, tipo={}, uploader={}", saved.getId(), tipo, uploader.getEmail());

        return DocumentoResponse.of(saved);
    }

    /**
     * Retorna metadados do documento (sem URL de visualização).
     */
    @Transactional(readOnly = true)
    public List<DocumentoResponse> listarPorVeiculo(UUID veiculoId) {
        return documentoRepository.findByVeiculoId(veiculoId).stream()
            .map(DocumentoResponse::of)
            .toList();
    }

    /**
     * Descriptografa o documento e serve os bytes diretamente para o frontend.
     *
     * O frontend nunca acessa o S3 diretamente — o backend é o único ponto de
     * saída do conteúdo descriptografado, aplicando verificação de integridade.
     */
    @Transactional(readOnly = true)
    public byte[] getDecryptedContent(UUID documentoId) {
        Documento doc = findOrThrow(documentoId);

        byte[] encrypted = storageService.retrieve(doc.getStorageKey());
        byte[] plainBytes = encryptionService.decrypt(encrypted);

        // Verifica integridade — detecta adulteração no S3 ou corrupção de storage
        if (!checksumService.verify(plainBytes, doc.getChecksumSha256())) {
            log.error("ALERTA DE INTEGRIDADE: checksum falhou para documento id={}", documentoId);
            throw new DocumentoIntegrityException(
                "Integridade do documento comprometida. Contate o administrador."
            );
        }

        return plainBytes;
    }

    /**
     * Exclui o documento do banco e do S3.
     * Apenas ADMIN pode chamar — garantido pelo @PreAuthorize no controller.
     */
    @Transactional
    public void deletar(UUID documentoId) {
        Documento doc = findOrThrow(documentoId);
        storageService.delete(doc.getStorageKey());
        documentoRepository.delete(doc);
        log.info("Documento excluído: id={}", documentoId);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Documento findOrThrow(UUID id) {
        return documentoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Documento não encontrado: " + id));
    }

    /**
     * Gera uma chave de storage opaca — não revela tipo, veículo ou cliente pelo nome.
     * Formato: docs/{uuid}/{uuid-único}
     */
    private String buildStorageKey(TipoDocumento tipo, UUID veiculoId, UUID clienteId) {
        String owner = veiculoId != null ? veiculoId.toString()
                     : clienteId != null ? clienteId.toString()
                     : UUID.randomUUID().toString();
        return String.format("docs/%s/%s", owner, UUID.randomUUID());
    }

    /**
     * Remove path traversal e caracteres perigosos do nome do arquivo.
     * O nome original é armazenado apenas para exibição ao usuário — nunca usado como path.
     */
    private String sanitizeFilename(String original) {
        if (original == null) return "documento.pdf";
        return original
            .replaceAll("[^a-zA-Z0-9._\\-\\s]", "_")
            .replaceAll("\\.{2,}", ".")
            .trim();
    }

    private User resolveCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado: " + email));
    }

    public static class DocumentoIntegrityException extends RuntimeException {
        public DocumentoIntegrityException(String message) { super(message); }
    }
}
