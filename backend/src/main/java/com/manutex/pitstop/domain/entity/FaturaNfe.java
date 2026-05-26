package com.manutex.pitstop.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faturas_nfe", indexes = {
    @Index(name = "idx_faturas_empresa", columnList = "empresa_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaturaNfe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "gateway_invoice_id", length = 255, unique = true)
    private String gatewayInvoiceId;

    @Column(name = "nfe_id", length = 255)
    private String nfeId;

    @Column(name = "nfe_status", length = 30)
    private String nfeStatus;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "xml_url", columnDefinition = "TEXT")
    private String xmlUrl;

    @CreationTimestamp
    @Column(name = "issue_date", nullable = false, updatable = false)
    private Instant issueDate;
}
