package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.entity.Cliente;
import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.entity.Veiculo;
import com.manutex.pitstop.domain.enums.StatusManutencao;
import com.manutex.pitstop.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ManutencaoResponseTest {

    private Veiculo veiculo;
    private User mecanico;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
            .nome("Maria Oliveira")
            .telefone("(11) 98765-4321")
            .build();

        veiculo = Veiculo.builder()
            .placa("XYZ5678")
            .marca("Toyota")
            .modelo("Corolla")
            .cor("Prata")
            .chassi("9BWZZZ372VT004251")
            .renavam("00258665599")
            .anoFabricacao(2022)
            .anoModelo(2023)
            .cliente(cliente)
            .build();

        mecanico = User.builder()
            .email("carlos@pitstop.com")
            .passwordHash("hash")
            .fullName("Carlos Mecânico")
            .role(UserRole.ROLE_MECANICO)
            .build();
    }

    @Test
    void deveMapeiarTodosOsCamposDaEntidade() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo)
            .mecanico(mecanico)
            .descricao("Revisão completa 60.000 km")
            .kmEntrada(60_000)
            .kmSaida(60_100)
            .relatorio("Trocados: óleo, filtros e correia dentada.")
            .orcamento(new BigDecimal("1200.00"))
            .valorFinal(new BigDecimal("1150.00"))
            .status(StatusManutencao.CONCLUIDA)
            .build();

        ManutencaoResponse resp = ManutencaoResponse.of(os);

        assertThat(resp.veiculoPlaca()).isEqualTo("XYZ5678");
        assertThat(resp.veiculoMarca()).isEqualTo("Toyota");
        assertThat(resp.veiculoModelo()).isEqualTo("Corolla");
        assertThat(resp.veiculoCor()).isEqualTo("Prata");
        assertThat(resp.clienteNome()).isEqualTo("Maria Oliveira");
        assertThat(resp.clienteTelefone()).isEqualTo("(11) 98765-4321");
        assertThat(resp.mecanicoNome()).isEqualTo("Carlos Mecânico");
        assertThat(resp.descricao()).isEqualTo("Revisão completa 60.000 km");
        assertThat(resp.kmEntrada()).isEqualTo(60_000);
        assertThat(resp.kmSaida()).isEqualTo(60_100);
        assertThat(resp.relatorio()).isEqualTo("Trocados: óleo, filtros e correia dentada.");
        assertThat(resp.orcamento()).isEqualByComparingTo("1200.00");
        assertThat(resp.valorFinal()).isEqualByComparingTo("1150.00");
        assertThat(resp.status()).isEqualTo(StatusManutencao.CONCLUIDA);
    }

    @Test
    void deveTratarVeiculoSemCliente() {
        veiculo = Veiculo.builder()
            .placa("AAA0000")
            .marca("Fiat")
            .modelo("Uno")
            .cor("Branco")
            .chassi("9BWZZZ372VT000001")
            .renavam("12345678901")
            .anoFabricacao(2018)
            .anoModelo(2019)
            .cliente(null)
            .build();

        Manutencao os = Manutencao.builder()
            .veiculo(veiculo)
            .mecanico(mecanico)
            .descricao("Check-up")
            .status(StatusManutencao.ABERTA)
            .build();

        ManutencaoResponse resp = ManutencaoResponse.of(os);

        assertThat(resp.clienteNome()).isNull();
        assertThat(resp.clienteTelefone()).isNull();
        assertThat(resp.veiculoPlaca()).isEqualTo("AAA0000");
    }

    @Test
    void deveTratarCamposOpcionaisNulos() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo)
            .mecanico(mecanico)
            .descricao("Serviço mínimo")
            .status(StatusManutencao.ABERTA)
            .build();

        ManutencaoResponse resp = ManutencaoResponse.of(os);

        assertThat(resp.kmSaida()).isNull();
        assertThat(resp.relatorio()).isNull();
        assertThat(resp.orcamento()).isNull();
        assertThat(resp.valorFinal()).isNull();
        assertThat(resp.dataConclusao()).isNull();
    }

    @Test
    void deveMapearStatusCorretamente() {
        for (StatusManutencao status : StatusManutencao.values()) {
            Manutencao os = Manutencao.builder()
                .veiculo(veiculo)
                .mecanico(mecanico)
                .descricao("Teste status " + status)
                .status(status)
                .build();

            ManutencaoResponse resp = ManutencaoResponse.of(os);
            assertThat(resp.status()).isEqualTo(status);
        }
    }
}
