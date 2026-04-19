package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.EmpresaConfig;
import com.manutex.pitstop.domain.repository.EmpresaConfigRepository;
import com.manutex.pitstop.web.dto.EmpresaConfigRequest;
import com.manutex.pitstop.web.dto.EmpresaConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaConfigService {

    private static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024;

    private final EmpresaConfigRepository repository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public EmpresaConfigResponse buscar() {
        return repository.findById(SINGLETON_ID)
            .map(e -> EmpresaConfigResponse.of(e, resolveLogoUrl(e)))
            .orElse(defaultConfig());
    }

    @Transactional
    public EmpresaConfigResponse salvar(EmpresaConfigRequest request) {
        EmpresaConfig config = repository.findById(SINGLETON_ID).orElseGet(this::newConfig);
        config.setNome(request.nome());
        config.setCnpj(request.cnpj());
        config.setEndereco(request.endereco());
        config.setTelefone(request.telefone());
        config.setEmail(request.email());
        EmpresaConfig saved = repository.save(config);
        log.info("Configuração da empresa salva: nome={}", saved.getNome());
        return EmpresaConfigResponse.of(saved, resolveLogoUrl(saved));
    }

    @Transactional
    public EmpresaConfigResponse uploadLogo(MultipartFile file) {
        validateLogo(file);
        EmpresaConfig config = repository.findById(SINGLETON_ID).orElseGet(this::newConfig);

        if (config.getLogoKey() != null) {
            storageService.delete(config.getLogoKey());
        }

        String key = "empresa/logo/" + UUID.randomUUID();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new LogoUploadException("Falha ao ler arquivo de logo.");
        }

        storageService.storeImage(key, bytes, file.getContentType());
        config.setLogoKey(key);
        config.setLogoMimeType(file.getContentType());

        EmpresaConfig saved = repository.save(config);
        log.info("Logo da empresa atualizado: key={}", key);
        return EmpresaConfigResponse.of(saved, resolveLogoUrl(saved));
    }

    private void validateLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new LogoUploadException("Arquivo de logo não pode ser vazio.");
        }
        String ct = file.getContentType();
        if (ct == null || (!ct.equals("image/png") && !ct.equals("image/jpeg") && !ct.equals("image/webp"))) {
            throw new LogoUploadException("Formato de logo inválido. Aceitos: PNG, JPEG, WebP.");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new LogoUploadException("Logo excede 2 MB.");
        }
    }

    private String resolveLogoUrl(EmpresaConfig e) {
        if (e.getLogoKey() == null) return null;
        try {
            return storageService.generatePresignedDownloadUrl(e.getLogoKey()).toString();
        } catch (Exception ex) {
            log.warn("Falha ao gerar URL do logo: {}", ex.getMessage());
            return null;
        }
    }

    private EmpresaConfig newConfig() {
        return EmpresaConfig.builder()
            .id(SINGLETON_ID)
            .nome("Minha Oficina")
            .build();
    }

    private EmpresaConfigResponse defaultConfig() {
        return new EmpresaConfigResponse(SINGLETON_ID, "Minha Oficina", null, null, null, null, null);
    }

    public static class LogoUploadException extends RuntimeException {
        public LogoUploadException(String message) { super(message); }
    }
}
