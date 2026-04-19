package com.manutex.pitstop.domain.entity;

import com.manutex.pitstop.domain.enums.TipoDocumento;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documentos", indexes = {
    @Index(name = "idx_documentos_veiculo",  columnList = "veiculo_id"),
    @Index(name = "idx_documentos_cliente",  columnList = "cliente_id"),
    @Index(name = "idx_documentos_storage",  columnList = "storage_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoDocumento tipo;

    /**
     * Caminho interno no bucket S3 — NUNCA exposto em respostas da API.
     * O frontend só recebe Pre-signed URLs temporárias geradas pelo backend.
     */
    @NotBlank
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @NotBlank
    @Size(max = 255)
    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    @Positive
    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @NotBlank
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /** SHA-256 do arquivo original — para verificação de integridade */
    @NotBlank
    @Size(min = 64, max = 64)
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Validade do documento físico (ex.: CRLV expira a cada ano) */
    @Column(name = "expires_at")
    private Instant expiresAt;
}
