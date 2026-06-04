package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.enums.NotificationChannel;
import com.manutex.pitstop.domain.enums.NotificationEvent;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Template de mensagem configurável por empresa, evento e canal.
 *
 * Variáveis disponíveis no corpo:
 *   {{cliente_nome}}, {{veiculo_placa}}, {{veiculo_modelo}},
 *   {{os_link}}, {{empresa_nome}}, {{status}}
 */
@Entity
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_notif_templates_empresa", columnList = "empresa_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationEvent evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel canal;

    @Column(length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String corpo;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
