package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Documento;
import com.manutex.pitstop.domain.enums.NotificationEvent;
import com.manutex.pitstop.domain.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Verifica diariamente documentos próximos do vencimento e
 * publica OsNotificationEvent para cada um, ativando o pipeline de notificação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    @Value("${app.notification.document-expiry-alert-days:7}")
    private int alertDays;

    private final DocumentoRepository      documentoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 8 * * ?")   // todos os dias às 08h
    @Transactional(readOnly = true)
    public void alertDocumentosVencendo() {
        Instant agora  = Instant.now();
        Instant limite = agora.plus(alertDays, ChronoUnit.DAYS);

        List<Documento> vencendo = documentoRepository.findVencendoEntre(agora, limite);
        log.info("Alertas de documentos: {} documento(s) vencendo nos próximos {} dias", vencendo.size(), alertDays);

        for (Documento doc : vencendo) {
            if (doc.getVeiculo() == null) continue;

            var veiculo = doc.getVeiculo();
            var cliente = veiculo.getCliente();
            if (cliente == null || doc.getUploadedBy() == null) continue;

            var empresa = doc.getUploadedBy().getEmpresa();
            if (empresa == null) continue;

            eventPublisher.publishEvent(new OsNotificationEvent(
                empresa.getId(),
                null,
                cliente.getId(),
                cliente.getTelefone(),
                cliente.getEmail(),
                cliente.getNome(),
                veiculo.getPlaca(),
                veiculo.getModelo(),
                null,
                NotificationEvent.DOCUMENTO_VENCENDO
            ));
        }
    }
}
