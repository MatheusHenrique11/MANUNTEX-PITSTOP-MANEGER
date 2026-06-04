package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.enums.FiscalEnvironment;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Dados fiscais da oficina/tenant.
 *
 * Fluxo A (SaaS): usado como TOMADOR quando a RiseCode emite NFS-e da assinatura.
 * Fluxo B (Workshop): usado como PRESTADOR quando a oficina emite NFS-e ao cliente final.
 *
 * NUNCA usar como prestador nas notas emitidas pela RiseCode Studio.
 * ROLE_GERENTE pode alterar apenas da própria empresa.
 */
@Entity
@Table(name = "tenant_fiscal_config", indexes = {
    @Index(name = "idx_tenant_fiscal_empresa", columnList = "empresa_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFiscalConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @Size(max = 200)
    @Column(name = "razao_social", length = 200)
    private String razaoSocial;

    @Size(max = 200)
    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @Size(max = 18)
    @Column(length = 18)
    private String cnpj;

    @Size(max = 30)
    @Column(name = "inscricao_municipal", length = 30)
    private String inscricaoMunicipal;

    @Size(max = 30)
    @Column(name = "regime_tributario", length = 30)
    private String regimeTributario;

    @Size(max = 10)
    @Column(name = "codigo_municipio", length = 10)
    private String codigoMunicipio;

    @Size(max = 100)
    @Column(length = 100)
    private String municipio;

    @Size(max = 2)
    @Column(length = 2)
    private String uf;

    @Size(max = 200)
    @Column(length = 200)
    private String endereco;

    @Size(max = 20)
    @Column(length = 20)
    private String numero;

    @Size(max = 100)
    @Column(length = 100)
    private String bairro;

    @Size(max = 10)
    @Column(length = 10)
    private String cep;

    @Email
    @Size(max = 180)
    @Column(name = "email_fiscal", length = 180)
    private String emailFiscal;

    @Size(max = 30)
    @Column(name = "telefone_fiscal", length = 30)
    private String telefoneFiscal;

    @Size(max = 30)
    @Column(name = "codigo_servico_municipal", length = 30)
    private String codigoServicoMunicipal;

    @Size(max = 10)
    @Column(name = "item_lista_servico", length = 10)
    private String itemListaServico;

    @Column(name = "aliquota_iss", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaIss = new BigDecimal("2.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente_fiscal", nullable = false, length = 20)
    @Builder.Default
    private FiscalEnvironment ambienteFiscal = FiscalEnvironment.HOMOLOGACAO;

    @Column(name = "fiscal_enabled", nullable = false)
    @Builder.Default
    private boolean fiscalEnabled = false;

    @Column(name = "fiscal_validated_at")
    private Instant fiscalValidatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Size(max = 180)
    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    public boolean isReadyForEmission() {
        return fiscalEnabled
            && cnpj != null && !cnpj.isBlank()
            && inscricaoMunicipal != null && !inscricaoMunicipal.isBlank()
            && codigoMunicipio != null && !codigoMunicipio.isBlank()
            && codigoServicoMunicipal != null && !codigoServicoMunicipal.isBlank()
            && itemListaServico != null && !itemListaServico.isBlank();
    }
}
