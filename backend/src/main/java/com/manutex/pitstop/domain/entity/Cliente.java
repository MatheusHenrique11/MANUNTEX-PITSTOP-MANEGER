package com.manutex.pitstop.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    /**
     * CPF/CNPJ armazenado em texto puro no banco.
     * Data Masking é feito na camada de resposta (DTO/MapStruct) —
     * apenas usuários com role ADMIN ou GERENTE recebem o valor completo.
     */
    @NotBlank
    @Pattern(
        regexp = "^(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}|\\d{11}|\\d{14})$",
        message = "CPF ou CNPJ inválido"
    )
    @Column(name = "cpf_cnpj", nullable = false, unique = true, length = 18)
    private String cpfCnpj;

    @Size(max = 20)
    @Pattern(regexp = "^\\+?[\\d\\s\\-()]{7,20}$", message = "Telefone inválido")
    @Column(length = 20)
    private String telefone;

    @Email
    @Size(max = 180)
    @Column(length = 180)
    private String email;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Veiculo> veiculos = new ArrayList<>();
}
