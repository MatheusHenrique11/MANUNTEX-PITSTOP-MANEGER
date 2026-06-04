package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Envia mensagens WhatsApp via Evolution API (https://evolution-api.com).
 *
 * Configuração por empresa (campos em empresa_notification_config):
 *   whatsapp_provider_url   → URL base da Evolution API
 *   whatsapp_api_token      → API Key (armazenada criptografada)
 *   whatsapp_instance_name  → Nome da instância conectada
 *
 * Segurança:
 *   - Números de telefone nunca aparecem completos nos logs (LGPD)
 *   - Timeout de 10 s para evitar thread starvation
 *   - WHATSAPP_ENABLED=false por padrão (modo mock em dev)
 */
@Slf4j
@Component
public class WhatsAppNotificationProvider implements NotificationProvider {

    private static final int TIMEOUT_MS = 10_000;

    @Value("${app.notification.whatsapp.enabled:false}")
    private boolean enabled;

    private final RestClient restClient;

    public WhatsAppNotificationProvider() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public NotificationResult send(String destinatario, String titulo, String corpo,
                                   Map<String, String> config) {
        String providerUrl  = config.get("whatsapp_provider_url");
        String apiToken     = config.get("whatsapp_api_token");
        String instanceName = config.get("whatsapp_instance_name");

        if (!StringUtils.hasText(providerUrl) || !StringUtils.hasText(apiToken)
                || !StringUtils.hasText(instanceName)) {
            return NotificationResult.fail("WhatsApp não configurado para esta empresa.");
        }

        if (!enabled) {
            log.info("[WhatsApp MOCK] Para={}**** enviado", maskPhone(destinatario));
            return NotificationResult.ok();
        }

        try {
            String url  = "%s/message/sendText/%s".formatted(providerUrl.stripTrailing(), instanceName);
            var    body = Map.of("number", sanitizePhone(destinatario), "text", corpo);

            restClient.post()
                .uri(url)
                .header("apikey", apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

            log.info("WhatsApp enviado para {}****", maskPhone(destinatario));
            return NotificationResult.ok();
        } catch (Exception e) {
            log.warn("Falha ao enviar WhatsApp para {}****: {}", maskPhone(destinatario), e.getMessage());
            return NotificationResult.fail(e.getMessage());
        }
    }

    private String sanitizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.startsWith("55") ? digits : "55" + digits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        return phone.substring(0, Math.min(3, phone.length() - 4));
    }
}
