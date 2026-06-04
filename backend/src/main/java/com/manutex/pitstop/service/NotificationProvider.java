package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.enums.NotificationChannel;

import java.util.Map;

/**
 * Porta de saída para envio de notificações.
 * Cada implementação suporta um canal específico (WhatsApp, e-mail, SMS…).
 * Novas implementações bastam registrar um @Component — nenhuma outra classe muda.
 */
public interface NotificationProvider {

    NotificationChannel getChannel();

    /**
     * @param destinatario telefone (WhatsApp/SMS) ou e-mail
     * @param titulo       assunto (e-mail) ou cabeçalho (WhatsApp)
     * @param corpo        corpo da mensagem com variáveis já interpoladas
     * @param config       configurações do provider para a empresa (URL, token, etc.)
     * @return resultado do envio
     */
    NotificationResult send(String destinatario, String titulo, String corpo,
                            Map<String, String> config);

    record NotificationResult(boolean success, String errorMessage) {
        static NotificationResult ok()                      { return new NotificationResult(true, null); }
        static NotificationResult fail(String errorMessage) { return new NotificationResult(false, errorMessage); }
    }
}
