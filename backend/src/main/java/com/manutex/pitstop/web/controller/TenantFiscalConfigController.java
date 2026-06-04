package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.service.TenantFiscalConfigService;
import com.manutex.pitstop.web.dto.TenantFiscalConfigRequest;
import com.manutex.pitstop.web.dto.TenantFiscalConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Dados fiscais da oficina/tenant (prestador nas NFS-e da oficina para o cliente final,
 * e tomador nas NFS-e da assinatura SaaS).
 *
 * ROLE_GERENTE gerencia apenas da própria empresa via TenantContext.
 * ROLE_ADMIN pode gerenciar qualquer tenant via /admin/fiscal/tenant/{empresaId}.
 */
@RestController
@RequiredArgsConstructor
public class TenantFiscalConfigController {

    private final TenantFiscalConfigService service;

    // ── Gerente gerencia apenas a própria empresa ─────────────────────────────

    @GetMapping("/api/v1/fiscal/tenant")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<TenantFiscalConfigResponse> buscar() {
        UUID empresaId = TenantContext.requireEmpresaId();
        return service.buscar(empresaId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/api/v1/fiscal/tenant")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<TenantFiscalConfigResponse> salvar(
        @Valid @RequestBody TenantFiscalConfigRequest request
    ) {
        UUID empresaId = TenantContext.requireEmpresaId();
        return ResponseEntity.ok(service.salvar(empresaId, request));
    }

    // ── Admin gerencia qualquer tenant ────────────────────────────────────────

    @GetMapping("/api/v1/admin/fiscal/tenant/{empresaId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TenantFiscalConfigResponse> buscarPorAdmin(
        @PathVariable UUID empresaId
    ) {
        return service.buscar(empresaId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/api/v1/admin/fiscal/tenant/{empresaId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TenantFiscalConfigResponse> salvarPorAdmin(
        @PathVariable UUID empresaId,
        @Valid @RequestBody TenantFiscalConfigRequest request
    ) {
        return ResponseEntity.ok(service.salvar(empresaId, request));
    }
}
