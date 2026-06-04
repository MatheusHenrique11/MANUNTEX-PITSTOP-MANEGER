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
 * Dados fiscais da RiseCode Studio usados como PRESTADOR nas NFS-e de assinatura SaaS.
 *
 * Linha única na tabela (a plataforma tem um único perfil fiscal).
 * Apenas ROLE_ADMIN pode alterar. Toda alteração gera AuditLog.
 */
@Entity
@Table(name = "platform_fiscal_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformFiscalConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Size(max = 200)
    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @NotBlank
    @Size(max = 18)
    @Column(nullable = false, length = 18)
    private String cnpj;

    @Size(max = 30)
    @Column(name = "inscricao_municipal", length = 30)
    private String inscricaoMunicipal;

    @Size(max = 30)
    @Column(name = "regime_tributario", length = 30)
    private String regimeTributario;

    @NotBlank
    @Size(max = 10)
    @Column(name = "codigo_municipio", nullable = false, length = 10)
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

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Size(max = 180)
    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    public boolean isReadyForProduction() {
        return razaoSocial != null && !razaoSocial.isBlank()
            && cnpj != null && !cnpj.isBlank()
            && inscricaoMunicipal != null && !inscricaoMunicipal.isBlank()
            && codigoMunicipio != null && !codigoMunicipio.isBlank()
            && codigoServicoMunicipal != null && !codigoServicoMunicipal.isBlank()
            && itemListaServico != null && !itemListaServico.isBlank()
            && aliquotaIss != null;
    }
}
