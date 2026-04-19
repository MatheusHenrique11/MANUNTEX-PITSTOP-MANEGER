package com.manutex.pitstop.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "empresa_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaConfig {

    @Id
    private UUID id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    @Size(max = 18)
    @Column(length = 18)
    private String cnpj;

    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String endereco;

    @Size(max = 20)
    @Column(length = 20)
    private String telefone;

    @Email
    @Size(max = 180)
    @Column(length = 180)
    private String email;

    @Column(name = "logo_key")
    private String logoKey;

    @Column(name = "logo_mime_type", length = 50)
    private String logoMimeType;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    private void touch() {
        updatedAt = Instant.now();
    }
}
