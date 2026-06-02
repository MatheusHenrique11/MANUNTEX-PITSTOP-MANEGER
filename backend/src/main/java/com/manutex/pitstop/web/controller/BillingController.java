package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.domain.enums.SubscriptionStatus;
import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.service.BillingService;
import com.manutex.pitstop.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /** Retorna uso atual de recursos e features do plano do tenant. */
    @GetMapping("/plan-usage")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<PlanUsageResponse> getPlanUsage() {
        UUID empresaId = TenantContext.requireEmpresaId();
        return ResponseEntity.ok(billingService.getPlanUsage(empresaId));
    }

    /** Retorna a assinatura mais recente do tenant logado. */
    @GetMapping("/subscription")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<AssinaturaResponse> getAssinatura() {
        UUID empresaId = TenantContext.requireEmpresaId();
        return ResponseEntity.ok(billingService.getAssinatura(empresaId));
    }

    /** Status da assinatura — usado pelo guard do Angular. */
    @GetMapping("/subscription/status")
    public ResponseEntity<SubscriptionStatus> getStatus() {
        UUID empresaId = TenantContext.requireEmpresaId();
        return ResponseEntity.ok(billingService.getStatus(empresaId));
    }

    /** Lista histórico de NFS-e / faturas do tenant. */
    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<List<FaturaNfeResponse>> getFaturas() {
        UUID empresaId = TenantContext.requireEmpresaId();
        return ResponseEntity.ok(billingService.getFaturas(empresaId));
    }

    /** Cria sessão de checkout no gateway de pagamento. */
    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<CheckoutResponse> criarCheckout(@Valid @RequestBody CheckoutRequest request) {
        UUID empresaId = TenantContext.requireEmpresaId();
        return ResponseEntity.ok(billingService.criarCheckout(empresaId, request.plano()));
    }

    /** Cancela a assinatura vigente ao final do período atual. */
    @DeleteMapping("/subscription")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_GERENTE')")
    public ResponseEntity<Void> cancelarAssinatura() {
        UUID empresaId = TenantContext.requireEmpresaId();
        billingService.cancelarAssinatura(empresaId);
        return ResponseEntity.noContent().build();
    }
}
