package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.service.LgpdService;
import com.manutex.pitstop.web.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de conformidade LGPD (Lei 13.709/2018).
 *
 * Rotas públicas (não exigem consentimento prévio):
 *   GET  /api/v1/lgpd/consent/status
 *   POST /api/v1/lgpd/consent
 *
 * Rotas protegidas (exigem autenticação):
 *   GET  /api/v1/lgpd/my-data     — portabilidade (Art. 18, V)
 *   POST /api/v1/lgpd/dsar        — solicitação do titular (Art. 18)
 *   GET  /api/v1/lgpd/dsar        — histórico de solicitações do usuário
 *
 * Rotas admin:
 *   GET  /api/v1/lgpd/admin/dsars              — todas as DSARs da empresa
 *   PUT  /api/v1/lgpd/admin/dsars/{id}/complete — concluir DSAR
 *   PUT  /api/v1/lgpd/admin/dsars/{id}/anonymize — anonimizar usuário (ERASURE)
 */
@RestController
@RequestMapping("/api/v1/lgpd")
@RequiredArgsConstructor
public class LgpdController {

    private final LgpdService lgpdService;

    // ── Consentimento ─────────────────────────────────────────────────────────

    @GetMapping("/consent/status")
    public ConsentStatusResponse getConsentStatus(Authentication auth) {
        UUID userId = TenantContext.requireUserId(auth);
        return lgpdService.getConsentStatus(userId);
    }

    @PostMapping("/consent")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentStatusResponse recordConsent(
        @Valid @RequestBody ConsentRequest request,
        Authentication auth,
        HttpServletRequest http
    ) {
        UUID userId    = TenantContext.requireUserId(auth);
        String ip      = resolveClientIp(http);
        String agent   = http.getHeader("User-Agent");
        return lgpdService.recordConsent(userId, request, ip, agent);
    }

    // ── Direitos do Titular ───────────────────────────────────────────────────

    @GetMapping("/my-data")
    public MyDataExport exportMyData(Authentication auth, HttpServletRequest http) {
        UUID userId = TenantContext.requireUserId(auth);
        return lgpdService.exportMyData(userId, auth, resolveClientIp(http));
    }

    @PostMapping("/dsar")
    @ResponseStatus(HttpStatus.CREATED)
    public DsarResponse submitDsar(
        @Valid @RequestBody DsarRequest request,
        Authentication auth,
        HttpServletRequest http
    ) {
        UUID userId = TenantContext.requireUserId(auth);
        return lgpdService.submitDsar(userId, request, resolveClientIp(http));
    }

    @GetMapping("/dsar")
    public List<DsarResponse> listMyDsars(Authentication auth) {
        UUID userId = TenantContext.requireUserId(auth);
        return lgpdService.listUserDsars(userId);
    }

    // ── Administração (ADMIN / GERENTE) ───────────────────────────────────────

    @GetMapping("/admin/dsars")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
    public List<DsarResponse> listAllDsars() {
        UUID empresaId = TenantContext.requireEmpresaId();
        return lgpdService.listAllDsars(empresaId);
    }

    @PutMapping("/admin/dsars/{id}/complete")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
    public DsarResponse completeDsar(
        @PathVariable UUID id,
        @RequestParam(required = false, defaultValue = "") String responseNotes,
        Authentication auth
    ) {
        return lgpdService.completeDsar(id, responseNotes, auth);
    }

    @PutMapping("/admin/dsars/{userId}/anonymize")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonymizeUser(@PathVariable UUID userId, Authentication auth) {
        lgpdService.anonymizeUser(userId, auth);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
