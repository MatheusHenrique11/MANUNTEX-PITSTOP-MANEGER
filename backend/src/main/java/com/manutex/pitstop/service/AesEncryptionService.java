package com.manutex.pitstop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Serviço de criptografia AES-256-GCM para documentos sensíveis.
 *
 * Usa AES/GCM/NoPadding (modo autenticado) em vez de AES/CBC porque:
 *  - GCM garante integridade: detecta adulteração do ciphertext sem chave extra
 *  - Não é vulnerável a padding oracle attacks
 *  - NIST recomendado para dados em repouso (NIST SP 800-38D)
 *
 * Formato do output: [12 bytes IV] + [ciphertext + 16 bytes auth tag]
 */
@Slf4j
@Service
public class AesEncryptionService {

    private static final String ALGORITHM     = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LENGTH = 12;   // 96 bits — recomendado NIST para GCM
    private static final int    GCM_TAG_BITS  = 128;  // 128-bit authentication tag
    private static final int    KEY_BITS      = 256;

    private final SecretKey masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptionService(
        @Value("${ENCRYPTION_MASTER_KEY}") String masterKeyBase64,
        @Value("${ENCRYPTION_SALT:pitstop-salt-v1}") String salt
    ) {
        this.masterKey = deriveKey(masterKeyBase64, salt);
    }

    /**
     * Criptografa bytes em memória.
     * @return [IV (12 bytes)] + [ciphertext + auth tag]
     */
    public byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);   // IV único por operação — crítico para segurança GCM

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext);

            // Concatena IV + ciphertext para armazenar juntos
            byte[] result = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, GCM_IV_LENGTH, ciphertext.length);
            return result;

        } catch (Exception e) {
            throw new EncryptionException("Falha na criptografia do documento", e);
        }
    }

    /**
     * Descriptografa bytes previamente cifrados por {@link #encrypt(byte[])}.
     * @param encryptedData [IV (12 bytes)] + [ciphertext + auth tag]
     */
    public byte[] decrypt(byte[] encryptedData) {
        try {
            if (encryptedData.length <= GCM_IV_LENGTH) {
                throw new EncryptionException("Dados criptografados inválidos ou corrompidos");
            }

            byte[] iv         = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            return cipher.doFinal(ciphertext);

        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            // Cobre AEADBadTagException — indica adulteração do ciphertext
            throw new EncryptionException("Falha na descriptografia: arquivo pode estar corrompido ou adulterado", e);
        }
    }

    /**
     * Deriva uma SecretKey de 256 bits a partir de uma senha usando PBKDF2.
     * Garante que mesmo uma chave mestra fraca gere uma chave AES forte.
     */
    private SecretKey deriveKey(String password, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt.getBytes(),
                310_000,    // iterações — OWASP recomenda ≥ 310.000 para PBKDF2-SHA256
                KEY_BITS
            );
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave de criptografia", e);
        }
    }

    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message) { super(message); }
        public EncryptionException(String message, Throwable cause) { super(message, cause); }
    }
}
