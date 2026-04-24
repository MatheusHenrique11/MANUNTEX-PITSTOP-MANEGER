package com.manutex.pitstop.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "metas_mecanico", indexes = {
    @Index(name = "idx_metas_mecanico_id", columnList = "mecanico_id"),
    @Index(name = "idx_metas_empresa_id",  columnList = "empresa_id"),
    @Index(name = "idx_metas_periodo",     columnList = "ano,mes")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaMecanico extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mecanico_id", nullable = false)
    private User mecanico;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Min(1) @Max(12)
    @Column(nullable = false)
    private int mes;

    @Min(2020)
    @Column(nullable = false)
    private int ano;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_meta", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMeta;
}
