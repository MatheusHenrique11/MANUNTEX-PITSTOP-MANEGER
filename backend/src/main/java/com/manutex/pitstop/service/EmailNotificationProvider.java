package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Envia e-mails via JavaMailSender (SMTP configurado em application.yml).
 *
 * Configuração por empresa (campos em empresa_config):
 *   notification_email_from → remetente customizado (ex: "Oficina XYZ <noreply@oficinaxyz.com.br>")
 *   Se não configurado, usa o MAIL_USERNAME global.
 *
 * Quando MAIL_ENABLED=false (padrão), os envios são apenas logados.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationProvider implements NotificationProvider {

    @Value("${app.notification.email.enabled:false}")
    private boolean enabled;

    @Value("${spring.mail.username:}")
    private String defaultFrom;

    private final JavaMailSender mailSender;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationResult send(String destinatario, String titulo, String corpo,
                                   Map<String, String> config) {
        if (!enabled) {
            log.info("[Email MOCK] Para=****@{} Assunto={}", domainOf(destinatario), titulo);
            return NotificationResult.ok();
        }

        String from = config.getOrDefault("notification_email_from", defaultFrom);
        if (from == null || from.isBlank()) {
            return NotificationResult.fail("Remetente de e-mail não configurado.");
        }

        try {
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(destinatario);
            message.setSubject(titulo != null ? titulo : "Notificação PitStop Manager");
            message.setText(corpo);
            mailSender.send(message);

            log.info("E-mail enviado para ****@{}", domainOf(destinatario));
            return NotificationResult.ok();
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail para ****@{}: {}", domainOf(destinatario), e.getMessage());
            return NotificationResult.fail(e.getMessage());
        }
    }

    private String domainOf(String email) {
        if (email == null || !email.contains("@")) return "?";
        return email.substring(email.lastIndexOf('@') + 1);
    }
}
