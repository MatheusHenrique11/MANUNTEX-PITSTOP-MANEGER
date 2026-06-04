package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.service.PlatformFiscalConfigService;
import com.manutex.pitstop.web.dto.PlatformFiscalConfigRequest;
import com.manutex.pitstop.web.dto.PlatformFiscalConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Dados fiscais da RiseCode Studio (prestador nas NFS-e de assinatura SaaS).
 * Acesso restrito a ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/fiscal/platform")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class PlatformFiscalConfigController {

    private final PlatformFiscalConfigService service;

    @GetMapping
    public ResponseEntity<PlatformFiscalConfigResponse> buscar() {
        return service.buscar()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<PlatformFiscalConfigResponse> salvar(
        @Valid @RequestBody PlatformFiscalConfigRequest request
    ) {
        return ResponseEntity.ok(service.salvar(request));
    }
}
