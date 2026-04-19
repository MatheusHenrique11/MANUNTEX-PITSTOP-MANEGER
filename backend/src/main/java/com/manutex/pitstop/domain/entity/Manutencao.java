package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.enums.StatusManutencao;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manutencoes", indexes = {
    @Index(name = "idx_manutencoes_veiculo",  columnList = "veiculo_id"),
    @Index(name = "idx_manutencoes_mecanico", columnList = "mecanico_id"),
    @Index(name = "idx_manutencoes_status",   columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manutencao extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mecanico_id", nullable = false)
    private User mecanico;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @PositiveOrZero
    @Column(name = "km_entrada")
    private Integer kmEntrada;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusManutencao status = StatusManutencao.ABERTA;

    @NotNull
    @Column(name = "data_entrada", nullable = false, updatable = false)
    @Builder.Default
    private Instant dataEntrada = Instant.now();

    @Column(name = "data_conclusao")
    private Instant dataConclusao;

    @Size(max = 2000)
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    /** Garante que data_conclusao é definida apenas quando status = CONCLUIDA */
    @PreUpdate
    @PrePersist
    private void validateConclusao() {
        if (status == StatusManutencao.CONCLUIDA && dataConclusao == null) {
            dataConclusao = Instant.now();
        }
        if (status != StatusManutencao.CONCLUIDA) {
            dataConclusao = null;
        }
    }
}
