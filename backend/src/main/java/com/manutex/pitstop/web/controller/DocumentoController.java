package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.domain.enums.TipoDocumento;
import com.manutex.pitstop.service.DocumentoService;
import com.manutex.pitstop.web.dto.DocumentoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    /**
     * Upload de documento PDF.
     * Requer autenticação (qualquer role pode fazer upload para veículos da oficina).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam("tipo") TipoDocumento tipo,
        @RequestParam(value = "veiculoId", required = false) UUID veiculoId,
        @RequestParam(value = "clienteId", required = false) UUID clienteId,
        @RequestParam(value = "expiresAt", required = false) Instant expiresAt
    ) {
        DocumentoResponse response = documentoService.upload(file, tipo, veiculoId, clienteId, expiresAt);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista metadados dos documentos de um veículo.
     * storageKey nunca aparece na resposta.
     */
    @GetMapping("/veiculo/{veiculoId}")
    public ResponseEntity<List<DocumentoResponse>> listarPorVeiculo(@PathVariable UUID veiculoId) {
        return ResponseEntity.ok(documentoService.listarPorVeiculo(veiculoId));
    }

    /**
     * Visualização do PDF — backend descriptografa e serve como stream.
     *
     * O PDF nunca fica exposto publicamente no S3.
     * O browser recebe os bytes diretamente do backend autenticado.
     * Adiciona Content-Disposition inline para abrir no browser (não forçar download).
     */
    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> viewDocument(@PathVariable UUID id) {
        byte[] pdfBytes = documentoService.getDecryptedContent(id);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"documento.pdf\"")
            // Impede que o browser armazene em cache o PDF sensível
            .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .body(pdfBytes);
    }

    /**
     * Exclusão física — apenas ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        documentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
