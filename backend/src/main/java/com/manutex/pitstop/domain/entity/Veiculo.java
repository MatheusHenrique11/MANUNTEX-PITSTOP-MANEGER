package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.validation.Chassi;
import com.manutex.pitstop.domain.validation.Placa;
import com.manutex.pitstop.domain.validation.Renavam;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "veiculos", indexes = {
    @Index(name = "idx_veiculos_cliente", columnList = "cliente_id"),
    @Index(name = "idx_veiculos_placa",   columnList = "placa",  unique = true),
    @Index(name = "idx_veiculos_chassi",  columnList = "chassi", unique = true),
    @Index(name = "idx_veiculos_renavam", columnList = "renavam", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Veiculo extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Placa normalizada em maiúsculo sem hífen (ex: ABC1234 ou ABC1D23).
     * Validada pelos padrões Antigo e Mercosul.
     */
    @NotBlank
    @Placa
    @Column(nullable = false, unique = true, length = 8)
    private String placa;

    /**
     * Chassi de 17 caracteres — validado pelo algoritmo ISO 3779.
     * Considerado dado sensível: mascarado em listagens gerais.
     */
    @NotBlank
    @Chassi
    @Column(nullable = false, unique = true, length = 17)
    private String chassi;

    /**
     * RENAVAM de 9 ou 11 dígitos — validado pelo algoritmo DENATRAN.
     * Considerado dado sensível: mascarado em listagens gerais.
     */
    @NotBlank
    @Renavam
    @Column(nullable = false, unique = true, length = 11)
    private String renavam;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String marca;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String modelo;

    @NotNull
    @Min(1900)
    @Max(2100)
    @Column(name = "ano_fabricacao", nullable = false)
    private Integer anoFabricacao;

    @NotNull
    @Min(1900)
    @Max(2100)
    @Column(name = "ano_modelo", nullable = false)
    private Integer anoModelo;

    @Size(max = 40)
    @Column(length = 40)
    private String cor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "veiculo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Manutencao> manutencoes = new ArrayList<>();

    @OneToMany(mappedBy = "veiculo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Documento> documentos = new ArrayList<>();

    /** Normaliza placa antes de persistir */
    @PrePersist
    @PreUpdate
    private void normalize() {
        if (placa != null) {
            placa = placa.toUpperCase().replaceAll("[\\s-]", "");
        }
        if (chassi != null) {
            chassi = chassi.toUpperCase().trim();
        }
    }
}
