package com.manutex.pitstop.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manutex.pitstop.service.BillingService;
import com.manutex.pitstop.service.StripePaymentGatewayService;
import com.manutex.pitstop.web.dto.StripeWebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Endpoint público para receber eventos do Stripe via Webhook.
 *
 * Segurança: valida o header Stripe-Signature via HMAC-SHA256 quando
 * STRIPE_WEBHOOK_SECRET está configurado. Sem o segredo, aceita em modo
 * desenvolvimento (apenas loggando aviso).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private static final String STRIPE_SIGNATURE_HEADER = "Stripe-Signature";

    private final BillingService billingService;
    private final StripePaymentGatewayService stripeService;
    private final ObjectMapper objectMapper;

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
        @RequestBody String payload,
        @RequestHeader(value = STRIPE_SIGNATURE_HEADER, required = false) String sigHeader
    ) {
        if (!validarAssinatura(payload, sigHeader)) {
            log.warn("Webhook Stripe rejeitado: assinatura inválida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StripeWebhookEvent event = objectMapper.readValue(payload, StripeWebhookEvent.class);
            log.info("Webhook Stripe recebido: type={} id={}", event.type(), event.id());
            despacharEvento(event);
        } catch (Exception e) {
            log.error("Erro ao processar webhook Stripe: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }

        return ResponseEntity.ok().build();
    }

    // ── Dispatcher ────────────────────────────────────────────────────────────

    private void despacharEvento(StripeWebhookEvent event) {
        switch (event.type()) {
            case "invoice.paid"                        -> billingService.processarPagamentoAprovado(event);
            case "invoice.payment_failed"              -> billingService.processarPagamentoFalhou(event);
            case "customer.subscription.deleted"       -> billingService.processarAssinaturaCancelada(event);
            default -> log.debug("Evento Stripe ignorado: {}", event.type());
        }
    }

    // ── Validação de assinatura HMAC-SHA256 ───────────────────────────────────

    private boolean validarAssinatura(String payload, String sigHeader) {
        String webhookSecret = stripeService.getWebhookSecret();

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("[DEV] STRIPE_WEBHOOK_SECRET não configurado — aceitando webhook sem validação de assinatura");
            return true;
        }
        if (sigHeader == null || sigHeader.isBlank()) {
            return false;
        }

        try {
            // Formato: t=timestamp,v1=hash
            String timestamp = extrairValor(sigHeader, "t");
            String receivedHash = extrairValor(sigHeader, "v1");
            if (timestamp == null || receivedHash == null) return false;

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expectedHash = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

            return expectedHash.equals(receivedHash);
        } catch (Exception e) {
            log.error("Falha ao validar assinatura do webhook: {}", e.getMessage());
            return false;
        }
    }

    private String extrairValor(String header, String chave) {
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(chave)) return kv[1];
        }
        return null;
    }
}
