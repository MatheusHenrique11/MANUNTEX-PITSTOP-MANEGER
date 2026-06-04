package com.manutex.pitstop.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Configuração de provedores de notificação por empresa (multi-tenant).
 * Separada de EmpresaConfig, que é singleton de configuração visual/fiscal.
 */
@Entity
@Table(name = "empresa_notification_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaNotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false, unique = true)
    private UUID empresaId;

    /** URL base da Evolution API (ex: https://api.evolution.suaempresa.com.br) */
    @Column(name = "whatsapp_provider_url", length = 500)
    private String whatsappProviderUrl;

    /** API Key da Evolution API. */
    @Column(name = "whatsapp_api_token", length = 500)
    private String whatsappApiToken;

    /** Nome da instância conectada na Evolution API. */
    @Column(name = "whatsapp_instance_name", length = 100)
    private String whatsappInstanceName;

    /** Remetente customizado de e-mail (ex: "Oficina XYZ <noreply@oficinaxyz.com.br>"). */
    @Column(name = "notification_email_from", length = 180)
    private String notificationEmailFrom;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
