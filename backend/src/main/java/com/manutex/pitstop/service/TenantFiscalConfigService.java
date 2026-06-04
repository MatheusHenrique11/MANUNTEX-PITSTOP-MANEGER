package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.AuditLog;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.TenantFiscalConfig;
import com.manutex.pitstop.domain.repository.AuditLogRepository;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.TenantFiscalConfigRepository;
import com.manutex.pitstop.web.dto.TenantFiscalConfigRequest;
import com.manutex.pitstop.web.dto.TenantFiscalConfigResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Gerencia os dados fiscais de cada tenant (oficina).
 * ROLE_GERENTE pode alterar apenas da própria empresa.
 * Toda alteração gera AuditLog.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantFiscalConfigService {

    private final TenantFiscalConfigRepository repository;
    private final EmpresaRepository empresaRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Optional<TenantFiscalConfigResponse> buscar(UUID empresaId) {
        return repository.findByEmpresaId(empresaId).map(this::toResponse);
    }

    @Transactional
    @SuppressWarnings("null") // JpaRepository.save/orElseGet retornam @NonNull por contrato; falso positivo do checker
    public TenantFiscalConfigResponse salvar(UUID empresaId, TenantFiscalConfigRequest request) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada: " + empresaId));

        TenantFiscalConfig config = repository.findByEmpresaId(empresaId)
            .orElseGet(() -> TenantFiscalConfig.builder().empresa(empresa).build());

        String actor = resolveActorEmail();
        boolean wasEnabled = config.isFiscalEnabled();
        applyRequest(config, request, actor);

        // Registra data de validação quando emissão é habilitada pela primeira vez
        if (!wasEnabled && config.isFiscalEnabled()) {
            config.setFiscalValidatedAt(Instant.now());
        }

        TenantFiscalConfig saved = repository.save(config);

        auditLogRepository.save(AuditLog.builder()
            .empresaId(empresaId)
            .userEmail(actor)
            .action("UPDATE")
            .resourceType("TENANT_FISCAL_CONFIG")
            .resourceId(saved.getId().toString())
            .detail("Configuração fiscal do tenant atualizada. fiscalEnabled=" + saved.isFiscalEnabled()
                + " ambiente=" + saved.getAmbienteFiscal())
            .build());

        log.info("[FISCAL-AUDIT] TenantFiscalConfig salva por={} empresa={} enabled={}",
            actor, empresaId, saved.isFiscalEnabled());

        return toResponse(saved);
    }

    private void applyRequest(TenantFiscalConfig c, TenantFiscalConfigRequest r, String actor) {
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
        if (r.ambienteFiscal() != null) c.setAmbienteFiscal(r.ambienteFiscal());
        c.setFiscalEnabled(r.fiscalEnabled());
        c.setUpdatedBy(actor);
    }

    private TenantFiscalConfigResponse toResponse(TenantFiscalConfig c) {
        return new TenantFiscalConfigResponse(
            c.getId(),
            c.getEmpresa().getId(),
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
            c.isFiscalEnabled(),
            c.isReadyForEmission(),
            c.getFiscalValidatedAt(),
            c.getUpdatedAt()
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
