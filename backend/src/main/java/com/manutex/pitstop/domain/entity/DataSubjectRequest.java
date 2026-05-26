package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.enums.DsarStatus;
import com.manutex.pitstop.domain.enums.DsarType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicitação do Titular de Dados (DSAR — Data Subject Access Request).
 * Cobre os direitos do Art. 18 da LGPD com prazo de 15 dias corridos.
 */
@Entity
@Table(name = "data_subject_requests", indexes = {
    @Index(name = "idx_dsr_empresa_id", columnList = "empresa_id"),
    @Index(name = "idx_dsr_status",     columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSubjectRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "requester_user_id")
    private UUID requesterUserId;

    @Column(name = "requester_email", nullable = false, length = 180)
    private String requesterEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private DsarType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DsarStatus status = DsarStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "processed_by", length = 180)
    private String processedBy;
}
