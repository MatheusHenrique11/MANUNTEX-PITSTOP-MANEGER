package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.enums.SubscriptionPlan;
import com.manutex.pitstop.domain.enums.SubscriptionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.br.CNPJ;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    @NotBlank
    @CNPJ(message = "CNPJ com dígitos verificadores inválidos")
    @Size(max = 18)
    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    // ── Dados fiscais do Tomador (NFS-e) ────────────────────────────────────

    @Size(max = 200)
    @Column(name = "razao_social", length = 200)
    private String razaoSocial;

    @Size(max = 200)
    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @Size(max = 10)
    @Column(length = 10)
    private String cep;

    @Size(max = 200)
    @Column(length = 200)
    private String logradouro;

    @Size(max = 20)
    @Column(length = 20)
    private String numero;

    @Size(max = 100)
    @Column(length = 100)
    private String complemento;

    @Size(max = 100)
    @Column(length = 100)
    private String bairro;

    @Size(max = 100)
    @Column(length = 100)
    private String cidade;

    @Size(max = 2)
    @Column(length = 2)
    private String uf;

    @Size(max = 10)
    @Column(name = "codigo_municipio_ibge", length = 10)
    private String codigoMunicipioIbge;

    @Size(max = 180)
    @Column(name = "email_fiscal", length = 180)
    private String emailFiscal;

    @Size(max = 20)
    @Column(name = "telefone_fiscal", length = 20)
    private String telefoneFiscal;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── Billing / Assinatura ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", length = 30)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "subscription_id", length = 255)
    private String subscriptionId;

    @Column(name = "payment_gateway_customer_id", length = 255)
    private String paymentGatewayCustomerId;
}
