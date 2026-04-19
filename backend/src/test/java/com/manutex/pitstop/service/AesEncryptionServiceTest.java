package com.manutex.pitstop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class AesEncryptionServiceTest {

    private AesEncryptionService service;

    @BeforeEach
    void setUp() {
        // Chave de 64 chars para satisfazer a validação mínima
        service = new AesEncryptionService(
            "test_key_for_unit_tests_only_must_be_64_chars_long_padded_12345678",
            "test-salt"
        );
    }

    @Test
    void deveCriptografarEDescriptografarCorretamente() {
        byte[] original = "Conteúdo de um PDF de teste".getBytes();
        byte[] encrypted = service.encrypt(original);
        byte[] decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void deveProduzirCiphertextsDiferentesParaMesmoPlaintext() {
        byte[] data = "mesmo conteudo".getBytes();
        byte[] enc1 = service.encrypt(data);
        byte[] enc2 = service.encrypt(data);

        // IV aleatório garante que duas encriptações do mesmo dado são diferentes
        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void deveDetectarAdulteracaoDosCiphertext() {
        byte[] original = "dado sensível".getBytes();
        byte[] encrypted = service.encrypt(original);

        // Adultera o último byte (auth tag)
        encrypted[encrypted.length - 1] ^= 0xFF;

        assertThatThrownBy(() -> service.decrypt(encrypted))
            .isInstanceOf(AesEncryptionService.EncryptionException.class)
            .hasMessageContaining("adulterado");
    }

    @Test
    void deveRejeitarDadosInsuficientes() {
        assertThatThrownBy(() -> service.decrypt(new byte[5]))
            .isInstanceOf(AesEncryptionService.EncryptionException.class);
    }
}
