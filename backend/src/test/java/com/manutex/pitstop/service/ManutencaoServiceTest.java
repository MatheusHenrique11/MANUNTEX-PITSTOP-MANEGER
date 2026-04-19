package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.*;
import com.manutex.pitstop.domain.enums.StatusManutencao;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.ManutencaoRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.domain.repository.VeiculoRepository;
import com.manutex.pitstop.web.dto.ManutencaoRequest;
import com.manutex.pitstop.web.dto.ManutencaoUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManutencaoServiceTest {

    @Mock ManutencaoRepository manutencaoRepository;
    @Mock VeiculoRepository veiculoRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ManutencaoService service;

    private UUID veiculoId;
    private UUID mecanicoId;
    private Veiculo veiculo;
    private User mecanico;

    @BeforeEach
    void setUp() {
        veiculoId = UUID.randomUUID();
        mecanicoId = UUID.randomUUID();

        Cliente cliente = Cliente.builder().nome("João Silva").telefone("(11) 99999-0000").build();
        veiculo = Veiculo.builder()
            .placa("ABC1234").chassi("9BWZZZ372VT004251").renavam("00258665599")
            .marca("VW").modelo("Gol").anoFabricacao(2020).anoModelo(2021)
            .cliente(cliente).build();

        mecanico = User.builder()
            .email("mecanico@pitstop.com").passwordHash("hash")
            .fullName("Carlos Mecânico").role(UserRole.ROLE_MECANICO).build();
    }

    @Test
    void deveCriarOsComCamposCompletos() {
        ManutencaoRequest request = new ManutencaoRequest(
            veiculoId, mecanicoId,
            "Revisão completa do motor e freios.",
            85000, null, "Trocar óleo",
            new BigDecimal("350.00"), null, null
        );

        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(userRepository.findById(mecanicoId)).thenReturn(Optional.of(mecanico));
        when(manutencaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.criar(request);

        assertThat(response.descricao()).isEqualTo("Revisão completa do motor e freios.");
        assertThat(response.orcamento()).isEqualByComparingTo("350.00");
        assertThat(response.status()).isEqualTo(StatusManutencao.ABERTA);
        assertThat(response.veiculoPlaca()).isEqualTo("ABC1234");
        assertThat(response.mecanicoNome()).isEqualTo("Carlos Mecânico");
        assertThat(response.clienteNome()).isEqualTo("João Silva");
    }

    @Test
    void deveLancarExcecaoQuandoVeiculoNaoEncontrado() {
        ManutencaoRequest request = new ManutencaoRequest(
            veiculoId, mecanicoId, "Descrição do serviço com 10+ chars.", null, null, null, null, null, null
        );
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Veículo não encontrado");
    }

    @Test
    void deveLancarExcecaoQuandoMecanicoNaoEncontrado() {
        ManutencaoRequest request = new ManutencaoRequest(
            veiculoId, mecanicoId, "Descrição do serviço com 10+ chars.", null, null, null, null, null, null
        );
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(userRepository.findById(mecanicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Mecânico não encontrado");
    }

    @Test
    void deveAtualizarRelatorioEOrcamento() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Serviço inicial completo.").status(StatusManutencao.EM_ANDAMENTO).build();

        when(manutencaoRepository.findById(any())).thenReturn(Optional.of(os));
        when(manutencaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManutencaoUpdateRequest update = new ManutencaoUpdateRequest(
            null, null, null, 86000,
            "Motor revisado, pastilhas de freio trocadas.",
            null, new BigDecimal("380.00"), new BigDecimal("380.00")
        );

        var response = service.atualizar(UUID.randomUUID(), update);

        assertThat(response.relatorio()).isEqualTo("Motor revisado, pastilhas de freio trocadas.");
        assertThat(response.valorFinal()).isEqualByComparingTo("380.00");
        assertThat(response.kmSaida()).isEqualTo(86000);
    }

    @Test
    void deveAlterarStatusParaEmAndamento() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Serviço completo de revisão.").status(StatusManutencao.ABERTA).build();

        when(manutencaoRepository.findById(any())).thenReturn(Optional.of(os));
        when(manutencaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.alterarStatus(UUID.randomUUID(), StatusManutencao.EM_ANDAMENTO);
        assertThat(response.status()).isEqualTo(StatusManutencao.EM_ANDAMENTO);
    }

    @Test
    void deveExigirRelatorioAoConcluir() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Serviço completo de revisão.").status(StatusManutencao.EM_ANDAMENTO).build();

        when(manutencaoRepository.findById(any())).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> service.alterarStatus(UUID.randomUUID(), StatusManutencao.CONCLUIDA))
            .isInstanceOf(ManutencaoService.StatusTransitionException.class)
            .hasMessageContaining("relatório");
    }

    @Test
    void deveConcluirComRelatorio() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Serviço completo de revisão.")
            .relatorio("Serviço realizado com sucesso.")
            .status(StatusManutencao.EM_ANDAMENTO).build();

        when(manutencaoRepository.findById(any())).thenReturn(Optional.of(os));
        when(manutencaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.alterarStatus(UUID.randomUUID(), StatusManutencao.CONCLUIDA);
        assertThat(response.status()).isEqualTo(StatusManutencao.CONCLUIDA);
    }

    @Test
    void deveRejeitarTransicaoDe_EstadoTerminal() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Serviço realizado com sucesso.")
            .status(StatusManutencao.CONCLUIDA).build();

        when(manutencaoRepository.findById(any())).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> service.alterarStatus(UUID.randomUUID(), StatusManutencao.ABERTA))
            .isInstanceOf(ManutencaoService.StatusTransitionException.class)
            .hasMessageContaining("terminal");
    }

    @Test
    void deveRejeitarEdicaoDeOsTerminal() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Revisão finalizada com sucesso.")
            .status(StatusManutencao.CANCELADA).build();

        when(manutencaoRepository.findById(any())).thenReturn(Optional.of(os));

        ManutencaoUpdateRequest update = new ManutencaoUpdateRequest(null, null, null, null, "Novo relatório.", null, null, null);

        assertThatThrownBy(() -> service.atualizar(UUID.randomUUID(), update))
            .isInstanceOf(ManutencaoService.StatusTransitionException.class);
    }

    @Test
    void deveListarPorVeiculoSemFiltroDeStatus() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Manutenção preventiva completa.").build();

        when(manutencaoRepository.findByVeiculoId(veiculoId, Pageable.unpaged()))
            .thenReturn(new PageImpl<>(List.of(os)));

        var page = service.listarPorVeiculo(veiculoId, null, Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deveDeletarOs() {
        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Revisão preventiva completa.").build();

        UUID id = UUID.randomUUID();
        when(manutencaoRepository.findById(id)).thenReturn(Optional.of(os));

        service.deletar(id);
        verify(manutencaoRepository).delete(os);
    }
}
