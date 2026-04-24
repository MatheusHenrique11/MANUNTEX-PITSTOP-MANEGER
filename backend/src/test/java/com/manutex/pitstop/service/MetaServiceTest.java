package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.*;
import com.manutex.pitstop.domain.enums.StatusManutencao;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.ManutencaoRepository;
import com.manutex.pitstop.domain.repository.MetaMecanicoRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.web.dto.MetaRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("null") // campos inicializados em @BeforeEach — IDE não rastreia
@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    @Mock MetaMecanicoRepository metaRepository;
    @Mock ManutencaoRepository manutencaoRepository;
    @Mock UserRepository userRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock PdfRelatorioService pdfRelatorioService;
    @Mock EmpresaConfigService empresaConfigService;

    @InjectMocks MetaService metaService;

    private UUID empresaId;
    private UUID mecanicoId;
    private UUID gerenteId;
    private Empresa empresa;
    private User mecanico;
    private User gerente;

    @BeforeEach
    void setUp() {
        empresaId = UUID.randomUUID();
        mecanicoId = UUID.randomUUID();
        gerenteId = UUID.randomUUID();

        empresa = Empresa.builder().nome("PitStop Ltda").cnpj("00.000.000/0001-00").build();
        ReflectionTestUtils.setField(empresa, "id", empresaId);

        mecanico = User.builder()
            .email("mecanico@pitstop.com")
            .passwordHash("hash")
            .fullName("Carlos Silva")
            .role(UserRole.ROLE_MECANICO)
            .empresa(empresa)
            .build();
        ReflectionTestUtils.setField(mecanico, "id", mecanicoId);

        gerente = User.builder()
            .email("gerente@pitstop.com")
            .passwordHash("hash")
            .fullName("Ana Gerente")
            .role(UserRole.ROLE_GERENTE)
            .empresa(empresa)
            .build();
        ReflectionTestUtils.setField(gerente, "id", gerenteId);
    }

    @Test
    void deveDefinirMetaComSucesso() {
        MetaRequest request = new MetaRequest(mecanicoId, 4, 2026, new BigDecimal("5000.00"));

        MetaMecanico metaSalva = MetaMecanico.builder()
            .mecanico(mecanico).empresa(empresa).mes(4).ano(2026)
            .valorMeta(new BigDecimal("5000.00")).build();

        try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            tc.when(TenantContext::requireEmpresaId).thenReturn(empresaId);

            when(userRepository.findById(mecanicoId)).thenReturn(Optional.of(mecanico));
            when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
            when(metaRepository.findByMecanicoIdAndMesAndAno(mecanicoId, 4, 2026))
                .thenReturn(Optional.empty());
            when(metaRepository.save(any())).thenReturn(metaSalva);

            var response = metaService.definirMeta(request);

            assertThat(response.mecanicoNome()).isEqualTo("Carlos Silva");
            assertThat(response.valorMeta()).isEqualByComparingTo("5000.00");
            assertThat(response.mes()).isEqualTo(4);
            assertThat(response.ano()).isEqualTo(2026);
        }
    }

    @Test
    void deveAtualizarMetaExistente() {
        MetaRequest request = new MetaRequest(mecanicoId, 4, 2026, new BigDecimal("7000.00"));

        MetaMecanico metaExistente = MetaMecanico.builder()
            .mecanico(mecanico).empresa(empresa).mes(4).ano(2026)
            .valorMeta(new BigDecimal("5000.00")).build();

        try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            tc.when(TenantContext::requireEmpresaId).thenReturn(empresaId);

            when(userRepository.findById(mecanicoId)).thenReturn(Optional.of(mecanico));
            when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
            when(metaRepository.findByMecanicoIdAndMesAndAno(mecanicoId, 4, 2026))
                .thenReturn(Optional.of(metaExistente));
            when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var response = metaService.definirMeta(request);
            assertThat(response.valorMeta()).isEqualByComparingTo("7000.00");
        }
    }

    @Test
    void deveRejeitarDefinirMetaParaUsuarioNaoMecanico() {
        MetaRequest request = new MetaRequest(gerenteId, 4, 2026, new BigDecimal("5000.00"));

        try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            tc.when(TenantContext::requireEmpresaId).thenReturn(empresaId);
            when(userRepository.findById(gerenteId)).thenReturn(Optional.of(gerente));

            assertThatThrownBy(() -> metaService.definirMeta(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não é um mecânico");
        }
    }

    @Test
    void deveCalcularProducaoComMetaBatida() {
        Cliente cliente = Cliente.builder().nome("João").telefone("11 99999-0000").build();
        Veiculo veiculo = Veiculo.builder()
            .placa("ABC1D23").chassi("9BWZZZ372VT004251").renavam("00258665599")
            .marca("VW").modelo("Gol").anoFabricacao(2020).anoModelo(2021)
            .cliente(cliente).build();

        Manutencao os1 = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Revisão completa").relatorio("Feito.")
            .valorFinal(new BigDecimal("3000.00"))
            .status(StatusManutencao.CONCLUIDA).build();

        Manutencao os2 = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Troca de freios e pastilhas").relatorio("Feito.")
            .valorFinal(new BigDecimal("2500.00"))
            .status(StatusManutencao.CONCLUIDA).build();

        MetaMecanico meta = MetaMecanico.builder()
            .mecanico(mecanico).empresa(empresa).mes(4).ano(2026)
            .valorMeta(new BigDecimal("5000.00")).build();

        try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            tc.when(TenantContext::requireEmpresaId).thenReturn(empresaId);
            mockSecurityContextWithRole("ROLE_GERENTE");

            when(userRepository.findById(mecanicoId)).thenReturn(Optional.of(mecanico));
            when(manutencaoRepository.findConcluidasByMecanicoAndPeriodo(mecanicoId, 4, 2026))
                .thenReturn(List.of(os1, os2));
            when(metaRepository.findByMecanicoIdAndMesAndAno(mecanicoId, 4, 2026))
                .thenReturn(Optional.of(meta));

            var producao = metaService.buscarProducaoMecanico(mecanicoId, 4, 2026);

            assertThat(producao.totalValorProduzido()).isEqualByComparingTo("5500.00");
            assertThat(producao.totalServicos()).isEqualTo(2);
            assertThat(producao.metaBatida()).isTrue();
            assertThat(producao.percentualAtingido()).isGreaterThan(100.0);
        }
    }

    @Test
    void deveCalcularProducaoSemMetaBatida() {
        Cliente cliente = Cliente.builder().nome("Maria").telefone("11 88888-0000").build();
        Veiculo veiculo = Veiculo.builder()
            .placa("XYZ9H87").chassi("9BWZZZ372VT009999").renavam("00111222333")
            .marca("Fiat").modelo("Uno").anoFabricacao(2019).anoModelo(2020)
            .cliente(cliente).build();

        Manutencao os = Manutencao.builder()
            .veiculo(veiculo).mecanico(mecanico)
            .descricao("Troca de óleo e filtros").relatorio("Feito.")
            .valorFinal(new BigDecimal("800.00"))
            .status(StatusManutencao.CONCLUIDA).build();

        MetaMecanico meta = MetaMecanico.builder()
            .mecanico(mecanico).empresa(empresa).mes(4).ano(2026)
            .valorMeta(new BigDecimal("5000.00")).build();

        try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            tc.when(TenantContext::requireEmpresaId).thenReturn(empresaId);
            mockSecurityContextWithRole("ROLE_GERENTE");

            when(userRepository.findById(mecanicoId)).thenReturn(Optional.of(mecanico));
            when(manutencaoRepository.findConcluidasByMecanicoAndPeriodo(mecanicoId, 4, 2026))
                .thenReturn(List.of(os));
            when(metaRepository.findByMecanicoIdAndMesAndAno(mecanicoId, 4, 2026))
                .thenReturn(Optional.of(meta));

            var producao = metaService.buscarProducaoMecanico(mecanicoId, 4, 2026);

            assertThat(producao.metaBatida()).isFalse();
            assertThat(producao.percentualAtingido()).isEqualTo(16.0);
        }
    }

    @Test
    void mecanicoNaoDeveVerProducaoDeOutroMecanico() {
        UUID outraMecanicoId = UUID.randomUUID();

        try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            tc.when(TenantContext::requireEmpresaId).thenReturn(empresaId);
            mockSecurityContextWithRole("ROLE_MECANICO");

            // idUsuarioAutenticado() resolves mecanico via email; getId() returns mecanicoId (set in @BeforeEach)
            when(userRepository.findByEmail("mecanico@pitstop.com"))
                .thenReturn(Optional.of(mecanico));

            // mecanicoId != outraMecanicoId → service deve lançar AccessDeniedException
            assertThatThrownBy(() -> metaService.buscarProducaoMecanico(outraMecanicoId, 4, 2026))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("própria produção");
        }
    }

    @Test
    void deveLancarExcecaoQuandoMecanicoNaoEncontrado() {
        MetaRequest request = new MetaRequest(mecanicoId, 4, 2026, new BigDecimal("5000.00"));

        try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            tc.when(TenantContext::requireEmpresaId).thenReturn(empresaId);
            when(userRepository.findById(mecanicoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> metaService.definirMeta(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Mecânico não encontrado");
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void mockSecurityContextWithRole(String role) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        GrantedAuthority authority = new SimpleGrantedAuthority(role);
        when(auth.getAuthorities()).thenAnswer(inv -> List.of(authority));
        // getName() só é chamado quando role == MECANICO; lenient evita UnnecessaryStubbing
        lenient().when(auth.getName()).thenReturn("mecanico@pitstop.com");
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ID do mecanico já definido em @BeforeEach via ReflectionTestUtils

}
