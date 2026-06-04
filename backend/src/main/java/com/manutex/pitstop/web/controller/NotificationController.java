package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.domain.enums.NotificationChannel;
import com.manutex.pitstop.domain.enums.NotificationEvent;
import com.manutex.pitstop.domain.enums.NotificationStatus;
import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.service.NotificationService;
import com.manutex.pitstop.web.dto.NotificationConfigRequest;
import com.manutex.pitstop.web.dto.NotificationConfigResponse;
import com.manutex.pitstop.web.dto.NotificationLogResponse;
import com.manutex.pitstop.web.dto.NotificationTemplateResponse;
import com.manutex.pitstop.web.dto.NotificationTemplateUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── Templates ─────────────────────────────────────────────────────────────

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<List<NotificationTemplateResponse>> listTemplates() {
        UUID empresaId = TenantContext.requireEmpresaId();
        var templates = notificationService.listTemplates(empresaId)
            .stream().map(NotificationTemplateResponse::of).toList();
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
        @PathVariable UUID id,
        @Valid @RequestBody NotificationTemplateUpdateRequest request
    ) {
        UUID empresaId = TenantContext.requireEmpresaId();
        var updated = notificationService.updateTemplate(
            id, empresaId, request.titulo(), request.corpo(), request.ativo());
        return ResponseEntity.ok(NotificationTemplateResponse.of(updated));
    }

    @PostMapping("/templates/seed")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> seedTemplates() {
        UUID empresaId = TenantContext.requireEmpresaId();
        notificationService.seedDefaultTemplates(empresaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/templates/{id}/test")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> testTemplate(
        @PathVariable UUID id,
        @RequestParam @NotBlank String destinatario
    ) {
        UUID empresaId = TenantContext.requireEmpresaId();
        notificationService.sendTest(id, empresaId, destinatario);
        return ResponseEntity.noContent().build();
    }

    // ── Configuração do provider ──────────────────────────────────────────────

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<NotificationConfigResponse> getConfig() {
        UUID empresaId = TenantContext.requireEmpresaId();
        return ResponseEntity.ok(NotificationConfigResponse.of(
            notificationService.getConfig(empresaId)));
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NotificationConfigResponse> saveConfig(
        @Valid @RequestBody NotificationConfigRequest request
    ) {
        UUID empresaId = TenantContext.requireEmpresaId();
        var saved = notificationService.saveConfig(
            empresaId,
            request.whatsappProviderUrl(),
            request.whatsappApiToken(),
            request.whatsappInstanceName(),
            request.notificationEmailFrom()
        );
        return ResponseEntity.ok(NotificationConfigResponse.of(saved));
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<Page<NotificationLogResponse>> listLogs(
        @RequestParam(required = false) NotificationStatus status,
        @RequestParam(required = false) NotificationChannel canal,
        @RequestParam(required = false) NotificationEvent   evento,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID empresaId = TenantContext.requireEmpresaId();
        return ResponseEntity.ok(
            notificationService.listLogs(empresaId, status, canal, evento, pageable)
                .map(NotificationLogResponse::of));
    }
}
