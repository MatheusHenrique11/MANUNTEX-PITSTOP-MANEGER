package com.manutex.pitstop.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manutex.pitstop.config.SecurityConfig;
import com.manutex.pitstop.config.TestSecurityConfig;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.service.MetaService;
import com.manutex.pitstop.web.dto.MetaRequest;
import com.manutex.pitstop.web.dto.MetaResponse;
import com.manutex.pitstop.web.dto.ProducaoMecanicoResponse;
import com.manutex.pitstop.web.dto.ServicoMecanicoItem;
import com.manutex.pitstop.web.filter.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = MetaController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@Import(TestSecurityConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
@SuppressWarnings("null")
class MetaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean MetaService metaService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean RateLimitFilter rateLimitFilter;

    private UUID mecanicoId;

    @BeforeEach
    void setUp() throws Exception {
        mecanicoId = UUID.randomUUID();

        doAnswer(inv -> {
            inv.<FilterChain>getArgument(2).doFilter(
                inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter)
            .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(inv -> {
            inv.<FilterChain>getArgument(2).doFilter(
                inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(rateLimitFilter)
            .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    // ── GERENTE define meta ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteDeveDefinirMetaComSucesso() throws Exception {
        MetaRequest req = new MetaRequest(mecanicoId, 4, 2026, new BigDecimal("5000.00"));
        MetaResponse resp = new MetaResponse(
            UUID.randomUUID(), mecanicoId, "Carlos Silva", 4, 2026,
            new BigDecimal("5000.00"), Instant.now(), Instant.now());

        when(metaService.definirMeta(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/metas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mecanicoNome").value("Carlos Silva"))
            .andExpect(jsonPath("$.valorMeta").value(5000.00))
            .andExpect(jsonPath("$.mes").value(4))
            .andExpect(jsonPath("$.ano").value(2026));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void mecanicoNaoDeveDefinirMeta() throws Exception {
        MetaRequest req = new MetaRequest(mecanicoId, 4, 2026, new BigDecimal("5000.00"));

        mockMvc.perform(post("/api/v1/metas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());

        verify(metaService, never()).definirMeta(any());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionistaNaoDeveDefinirMeta() throws Exception {
        MetaRequest req = new MetaRequest(mecanicoId, 4, 2026, new BigDecimal("5000.00"));

        mockMvc.perform(post("/api/v1/metas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());

        verify(metaService, never()).definirMeta(any());
    }

    // ── GERENTE lista produção geral ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteDeveListarProducaoGeral() throws Exception {
        ProducaoMecanicoResponse prod = producaoMock(mecanicoId, "Carlos Silva", true);
        when(metaService.listarProducaoTodosMecanicos(anyInt(), anyInt()))
            .thenReturn(List.of(prod));

        mockMvc.perform(get("/api/v1/metas").param("mes", "4").param("ano", "2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].mecanicoNome").value("Carlos Silva"))
            .andExpect(jsonPath("$[0].metaBatida").value(true));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionistaNaoDeveListarProducaoGeral() throws Exception {
        mockMvc.perform(get("/api/v1/metas").param("mes", "4").param("ano", "2026"))
            .andExpect(status().isForbidden());

        verify(metaService, never()).listarProducaoTodosMecanicos(anyInt(), anyInt());
    }

    // ── GERENTE vê detalhes de um mecânico ───────────────────────────────────

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteDeveVerDetalhesDeQualquerMecanico() throws Exception {
        ProducaoMecanicoResponse prod = producaoMock(mecanicoId, "Carlos Silva", false);
        when(metaService.buscarProducaoMecanico(eq(mecanicoId), anyInt(), anyInt()))
            .thenReturn(prod);

        mockMvc.perform(get("/api/v1/metas/mecanico/{id}", mecanicoId)
                .param("mes", "4").param("ano", "2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mecanicoId").value(mecanicoId.toString()))
            .andExpect(jsonPath("$.metaBatida").value(false));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionistaNaoDeveVerDetalhesMecanico() throws Exception {
        mockMvc.perform(get("/api/v1/metas/mecanico/{id}", mecanicoId)
                .param("mes", "4").param("ano", "2026"))
            .andExpect(status().isForbidden());
    }

    // ── MECANICO vê apenas seus próprios dados ────────────────────────────────

    @Test
    @WithMockUser(roles = "MECANICO")
    void mecanicoDeveVerApenasMinhaProducao() throws Exception {
        ProducaoMecanicoResponse prod = producaoMock(mecanicoId, "Carlos Silva", true);
        when(metaService.buscarMinhaProducao(anyInt(), anyInt())).thenReturn(prod);

        mockMvc.perform(get("/api/v1/metas/minhas").param("mes", "4").param("ano", "2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mecanicoNome").value("Carlos Silva"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void mecanicoDeveReceberAcessoNegadoAoDadosDeOutro() throws Exception {
        when(metaService.buscarProducaoMecanico(eq(mecanicoId), anyInt(), anyInt()))
            .thenThrow(new AccessDeniedException("Mecânico só pode consultar sua própria produção."));

        mockMvc.perform(get("/api/v1/metas/mecanico/{id}", mecanicoId)
                .param("mes", "4").param("ano", "2026"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionistaNaoDeveAcessarRotaMinhas() throws Exception {
        mockMvc.perform(get("/api/v1/metas/minhas"))
            .andExpect(status().isForbidden());

        verify(metaService, never()).buscarMinhaProducao(anyInt(), anyInt());
    }

    // ── PDF para RH ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteDeveGerarPdf() throws Exception {
        byte[] pdfBytes = "PDF-FAKE-CONTENT".getBytes();
        when(metaService.gerarPdfRelatorio(anyInt(), anyInt())).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/metas/relatorio/pdf")
                .param("mes", "4").param("ano", "2026"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"relatorio-metas-04-2026.pdf\""))
            .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void mecanicoNaoDeveGerarPdf() throws Exception {
        mockMvc.perform(get("/api/v1/metas/relatorio/pdf").param("mes", "4").param("ano", "2026"))
            .andExpect(status().isForbidden());

        verify(metaService, never()).gerarPdfRelatorio(anyInt(), anyInt());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionistaNaoDeveGerarPdf() throws Exception {
        mockMvc.perform(get("/api/v1/metas/relatorio/pdf").param("mes", "4").param("ano", "2026"))
            .andExpect(status().isForbidden());

        verify(metaService, never()).gerarPdfRelatorio(anyInt(), anyInt());
    }

    // ── Validação de payload ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "GERENTE")
    void deveRetornar422ComPayloadInvalido() throws Exception {
        // valorMeta negativo — viola @DecimalMin("0.01")
        // GlobalExceptionHandler mapeia MethodArgumentNotValidException → 422
        String json = """
            {"mecanicoId":"%s","mes":4,"ano":2026,"valorMeta":-100.00}
            """.formatted(mecanicoId);

        mockMvc.perform(post("/api/v1/metas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isUnprocessableEntity());
    }

    // ── factory ───────────────────────────────────────────────────────────────

    private ProducaoMecanicoResponse producaoMock(UUID id, String nome, boolean metaBatida) {
        return new ProducaoMecanicoResponse(
            id, nome, 4, 2026, 3,
            new BigDecimal("5500.00"), new BigDecimal("5000.00"),
            110.0, metaBatida,
            List.of(new ServicoMecanicoItem(
                UUID.randomUUID(), "ABC1D23", "VW", "Gol", "João",
                "Revisão completa", new BigDecimal("2000.00"),
                Instant.now(), com.manutex.pitstop.domain.enums.StatusManutencao.CONCLUIDA
            ))
        );
    }
}
