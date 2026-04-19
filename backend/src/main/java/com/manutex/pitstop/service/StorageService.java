package com.manutex.pitstop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

/**
 * Camada de abstração sobre o S3/MinIO.
 *
 * Todos os objetos são armazenados com ServerSideEncryption.AES256 (SSE-S3).
 * Para ambientes de produção críticos, trocar por SSE-KMS com chave gerenciada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.storage.bucket}")
    private String bucket;

    @Value("${app.storage.presigned-url-expiry-minutes:5}")
    private long presignedExpiryMinutes;

    /**
     * Armazena bytes (já criptografados pela app) no S3 com SSE-S3 adicional.
     * A dupla criptografia (app + S3) garante "defense in depth":
     * mesmo que o bucket S3 seja exposto, os dados continuam cifrados pela chave da aplicação.
     */
    public void store(String key, byte[] encryptedBytes, String originalMimeType) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType("application/octet-stream")   // opaco — não revela que é PDF
            .serverSideEncryption(ServerSideEncryption.AES256)
            .contentLength((long) encryptedBytes.length)
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(encryptedBytes));
        log.info("Documento armazenado: bucket={}, key={}, size={}B", bucket, key, encryptedBytes.length);
    }

    /**
     * Recupera os bytes criptografados do S3.
     * A descriptografia é feita pelo chamador (DocumentoService).
     */
    public byte[] retrieve(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        try {
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new DocumentNotFoundException("Documento não encontrado no storage: " + key);
        }
    }

    /**
     * Gera uma Pre-signed URL com expiração curta para leitura segura.
     *
     * A URL é gerada diretamente para o objeto criptografado.
     * ATENÇÃO: Esta URL dá acesso ao ciphertext, não ao PDF legível.
     * Para exibir o PDF no browser, o backend deve descriptografar e servir via stream.
     *
     * @see DocumentoService#gerarUrlVisualizacao(java.util.UUID, java.util.UUID)
     */
    public URL generatePresignedDownloadUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(presignedExpiryMinutes))
            .getObjectRequest(r -> r.bucket(bucket).key(key))
            .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        log.info("Documento removido do storage: bucket={}, key={}", bucket, key);
    }

    public static class DocumentNotFoundException extends RuntimeException {
        public DocumentNotFoundException(String message) { super(message); }
    }
}
