package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.enums.ConsentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro imutável de consentimento do titular (Art. 8 LGPD).
 * Cada linha é append-only — nunca atualizada, apenas inserida.
 */
@Entity
@Table(name = "user_consents", indexes = {
    @Index(name = "idx_user_consents_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 50)
    private ConsentType policyType;

    @Column(name = "policy_version", nullable = false, length = 20)
    private String policyVersion;

    @Column(nullable = false)
    private boolean accepted;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
