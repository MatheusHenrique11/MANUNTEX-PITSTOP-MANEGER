package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.Cliente;
import com.manutex.pitstop.domain.entity.Veiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VeiculoResponseTest {

    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente();

        veiculo = Veiculo.builder()
            .placa("ABC1234")
            .chassi("9BWZZZ377VT004251")
            .renavam("00258665590")
            .marca("Volkswagen")
            .modelo("Gol")
            .anoFabricacao(2020)
            .anoModelo(2021)
            .cor("Prata")
            .cliente(cliente)
            .build();

        // Força ID para facilitar asserção
        try {
            var idField = Veiculo.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(veiculo, UUID.randomUUID());
        } catch (Exception ignored) {}
    }

    @Test
    void deveExporChassiCompletoParaPrivilegiado() {
        VeiculoResponse response = VeiculoResponse.of(veiculo, true);
        assertThat(response.chassi()).isEqualTo("9BWZZZ377VT004251");
        assertThat(response.renavam()).isEqualTo("00258665590");
    }

    @Test
    void deveMascararChassiParaUsuarioComum() {
        VeiculoResponse response = VeiculoResponse.of(veiculo, false);
        // Só os últimos 4 chars visíveis
        assertThat(response.chassi()).endsWith("4251");
        assertThat(response.chassi()).startsWith("*");
        assertThat(response.chassi()).doesNotContain("9BWZZZ377VT0");
    }

    @Test
    void deveMascararRenavamParaUsuarioComum() {
        VeiculoResponse response = VeiculoResponse.of(veiculo, false);
        // Só os últimos 3 chars visíveis
        assertThat(response.renavam()).endsWith("590");
        assertThat(response.renavam()).startsWith("*");
    }

    @Test
    void deveManter17CharsNoChassimMascarado() {
        VeiculoResponse response = VeiculoResponse.of(veiculo, false);
        // Comprimento original do chassi deve ser mantido
        assertThat(response.chassi()).hasSize(17);
    }

    @Test
    void deveExporPlacaSemMascaramento() {
        VeiculoResponse response = VeiculoResponse.of(veiculo, false);
        assertThat(response.placa()).isEqualTo("ABC1234");
    }

    @Test
    void deveExporDadosBasicosCorretamente() {
        VeiculoResponse response = VeiculoResponse.of(veiculo, false);
        assertThat(response.marca()).isEqualTo("Volkswagen");
        assertThat(response.modelo()).isEqualTo("Gol");
        assertThat(response.anoFabricacao()).isEqualTo(2020);
        assertThat(response.anoModelo()).isEqualTo(2021);
        assertThat(response.cor()).isEqualTo("Prata");
    }
}
