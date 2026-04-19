package com.manutex.pitstop.service;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Gera e verifica checksums SHA-256 para garantir integridade de documentos.
 * Calcula sobre o conteúdo ORIGINAL (antes da criptografia) para permitir
 * verificação sem precisar descriptografar.
 */
@Service
public class ChecksumService {

    public String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }

    public boolean verify(byte[] data, String expectedHex) {
        return sha256Hex(data).equalsIgnoreCase(expectedHex);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
