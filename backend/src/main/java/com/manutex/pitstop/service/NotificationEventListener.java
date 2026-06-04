package com.manutex.pitstop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Ouve eventos de domínio e despacha notificações de forma assíncrona
 * APÓS o commit da transação principal (garantindo que a OS está no banco).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOsEvent(OsNotificationEvent event) {
        log.debug("Processando evento de notificação: {} para OS={}", event.evento(), event.manutencaoId());
        try {
            notificationService.dispatch(event);
        } catch (Exception e) {
            log.warn("Falha ao processar notificação para OS={}: {}", event.manutencaoId(), e.getMessage());
        }
    }
}
