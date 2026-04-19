package com.manutex.pitstop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChecksumServiceTest {

    private ChecksumService service;

    @BeforeEach
    void setUp() {
        service = new ChecksumService();
    }

    @Test
    void deveGerarHashSha256Correto() {
        // SHA-256 de "abc" é conhecido
        byte[] data = "abc".getBytes();
        String hash = service.sha256Hex(data);
        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void deveProuzirHashDe64Chars() {
        byte[] data = "qualquer conteudo".getBytes();
        String hash = service.sha256Hex(data);
        assertThat(hash).hasSize(64);
    }

    @Test
    void deveVerificarChecksumValido() {
        byte[] data = "documento pdf".getBytes();
        String hash = service.sha256Hex(data);
        assertThat(service.verify(data, hash)).isTrue();
    }

    @Test
    void deveRejeitarChecksumInvalido() {
        byte[] data = "documento pdf".getBytes();
        assertThat(service.verify(data, "hash_errado_qualquer")).isFalse();
    }

    @Test
    void deveProuzirHashesDiferentesParaDadosDiferentes() {
        byte[] d1 = "conteudo A".getBytes();
        byte[] d2 = "conteudo B".getBytes();
        assertThat(service.sha256Hex(d1)).isNotEqualTo(service.sha256Hex(d2));
    }

    @Test
    void deveSerDeterministico() {
        byte[] data = "mesmo dado".getBytes();
        assertThat(service.sha256Hex(data)).isEqualTo(service.sha256Hex(data));
    }
}
