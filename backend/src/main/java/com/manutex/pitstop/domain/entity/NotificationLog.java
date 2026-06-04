package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.enums.NotificationChannel;
import com.manutex.pitstop.domain.enums.NotificationEvent;
import com.manutex.pitstop.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro imutável de cada tentativa de envio de notificação.
 * destinatario é mascarado (LGPD): apenas últimos 4 dígitos/chars visíveis.
 */
@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_notif_logs_empresa",    columnList = "empresa_id"),
    @Index(name = "idx_notif_logs_manutencao", columnList = "manutencao_id"),
    @Index(name = "idx_notif_logs_created",    columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "manutencao_id")
    private UUID manutencaoId;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationEvent evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel canal;

    /** Mascarado — ex: "***-**34" para WhatsApp, "***@gmail.com" para e-mail. */
    @Column(length = 200)
    private String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDENTE;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
