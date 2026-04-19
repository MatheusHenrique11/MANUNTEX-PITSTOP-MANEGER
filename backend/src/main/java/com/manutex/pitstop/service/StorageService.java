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

    public void store(String key, byte[] encryptedBytes, String originalMimeType) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType("application/octet-stream")
            .serverSideEncryption(ServerSideEncryption.AES256)
            .contentLength((long) encryptedBytes.length)
            .build();
        s3Client.putObject(request, RequestBody.fromBytes(encryptedBytes));
        log.info("Documento armazenado: bucket={}, key={}, size={}B", bucket, key, encryptedBytes.length);
    }

    public void storeImage(String key, byte[] bytes, String mimeType) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(mimeType)
            .serverSideEncryption(ServerSideEncryption.AES256)
            .contentLength((long) bytes.length)
            .build();
        s3Client.putObject(request, RequestBody.fromBytes(bytes));
        log.info("Imagem armazenada: bucket={}, key={}, size={}B", bucket, key, bytes.length);
    }

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

    public URL generatePresignedDownloadUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(presignedExpiryMinutes))
            .getObjectRequest(r -> r.bucket(bucket).key(key))
            .build();
        return s3Presigner.presignGetObject(presignRequest).url();
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        log.info("Objeto removido do storage: bucket={}, key={}", bucket, key);
    }

    public static class DocumentNotFoundException extends RuntimeException {
        public DocumentNotFoundException(String message) { super(message); }
    }
}
