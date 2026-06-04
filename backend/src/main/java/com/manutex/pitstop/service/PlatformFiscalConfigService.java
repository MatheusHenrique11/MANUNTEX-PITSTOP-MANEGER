package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.AuditLog;
import com.manutex.pitstop.domain.entity.PlatformFiscalConfig;
import com.manutex.pitstop.domain.repository.AuditLogRepository;
import com.manutex.pitstop.domain.repository.PlatformFiscalConfigRepository;
import com.manutex.pitstop.web.dto.PlatformFiscalConfigRequest;
import com.manutex.pitstop.web.dto.PlatformFiscalConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Gerencia os dados fiscais da RiseCode Studio (plataforma).
 * Apenas ROLE_ADMIN pode alterar. Toda alteração gera AuditLog.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformFiscalConfigService {

    private final PlatformFiscalConfigRepository repository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Optional<PlatformFiscalConfigResponse> buscar() {
        return repository.findFirstByOrderByUpdatedAtDesc()
            .map(this::toResponse);
    }

    @Transactional
    @SuppressWarnings("null") // JpaRepository.save/orElseGet retornam @NonNull por contrato; falso positivo do checker
    public PlatformFiscalConfigResponse salvar(PlatformFiscalConfigRequest request) {
        PlatformFiscalConfig config = repository.findFirstByOrderByUpdatedAtDesc()
            .orElseGet(PlatformFiscalConfig::new);

        String actor = resolveActorEmail();
        applyRequest(config, request, actor);
        PlatformFiscalConfig saved = repository.save(config);

        auditLogRepository.save(AuditLog.builder()
            .userEmail(actor)
            .action("UPDATE")
            .resourceType("PLATFORM_FISCAL_CONFIG")
            .resourceId(saved.getId().toString())
            .detail("Configuração fiscal da plataforma atualizada. Ambiente: " + saved.getAmbienteFiscal())
            .build());

        log.info("[FISCAL-AUDIT] PlatformFiscalConfig salva por={} ambiente={}", actor, saved.getAmbienteFiscal());
        return toResponse(saved);
    }

    private void applyRequest(PlatformFiscalConfig c, PlatformFiscalConfigRequest r, String actor) {
        c.setRazaoSocial(r.razaoSocial());
        c.setNomeFantasia(r.nomeFantasia());
        c.setCnpj(r.cnpj() != null ? r.cnpj().replaceAll("[^0-9]", "") : null);
        c.setInscricaoMunicipal(r.inscricaoMunicipal());
        c.setRegimeTributario(r.regimeTributario());
        c.setCodigoMunicipio(r.codigoMunicipio());
        c.setMunicipio(r.municipio());
        c.setUf(r.uf());
        c.setEndereco(r.endereco());
        c.setNumero(r.numero());
        c.setBairro(r.bairro());
        c.setCep(r.cep() != null ? r.cep().replaceAll("[^0-9]", "") : null);
        c.setEmailFiscal(r.emailFiscal());
        c.setTelefoneFiscal(r.telefoneFiscal());
        c.setCodigoServicoMunicipal(r.codigoServicoMunicipal());
        c.setItemListaServico(r.itemListaServico());
        c.setAliquotaIss(r.aliquotaIss());
        c.setAmbienteFiscal(r.ambienteFiscal());
        c.setUpdatedBy(actor);
    }

    private PlatformFiscalConfigResponse toResponse(PlatformFiscalConfig c) {
        return new PlatformFiscalConfigResponse(
            c.getId(),
            c.getRazaoSocial(),
            c.getNomeFantasia(),
            maskCnpj(c.getCnpj()),
            c.getInscricaoMunicipal(),
            c.getRegimeTributario(),
            c.getCodigoMunicipio(),
            c.getMunicipio(),
            c.getUf(),
            c.getEndereco(),
            c.getNumero(),
            c.getBairro(),
            c.getCep(),
            c.getEmailFiscal(),
            c.getTelefoneFiscal(),
            c.getCodigoServicoMunicipal(),
            c.getItemListaServico(),
            c.getAliquotaIss(),
            c.getAmbienteFiscal(),
            c.isReadyForProduction(),
            c.getUpdatedAt(),
            c.getUpdatedBy()
        );
    }

    private String resolveActorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private String maskCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) return null;
        String digits = cnpj.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "***";
        return digits.substring(0, 2) + ".***.***/" + digits.substring(digits.length() - 6, digits.length() - 2) + "-**";
    }
}
