package com.manutex.pitstop.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manutex.pitstop.config.SecurityConfig;
import com.manutex.pitstop.config.TestSecurityConfig;
import com.manutex.pitstop.domain.enums.StatusManutencao;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.service.EmpresaConfigService;
import com.manutex.pitstop.service.ManutencaoService;
import com.manutex.pitstop.web.dto.*;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = ManutencaoController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@Import(TestSecurityConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class ManutencaoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ManutencaoService manutencaoService;
    @MockBean EmpresaConfigService empresaConfigService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean RateLimitFilter rateLimitFilter;

    @BeforeEach
    void configureMockFilters() throws Exception {
        doAnswer(inv -> { inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        doAnswer(inv -> { inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(rateLimitFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    private ManutencaoResponse osResponse() {
        UUID id = UUID.randomUUID();
        return new ManutencaoResponse(
            id, UUID.randomUUID(), UUID.randomUUID(), "ABC1234", "VW", "Gol", "Prata",
            "João Silva", "(11) 99999-0000",
            UUID.randomUUID(), "Carlos Mecânico",
            "Revisão completa do motor e sistema de freios.",
            85000, null, null, null,
            new BigDecimal("350.00"), null,
            StatusManutencao.ABERTA,
            Instant.now(), null, Instant.now(), Instant.now()
        );
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveCriarOs() throws Exception {
        ManutencaoRequest request = new ManutencaoRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "Revisão completa do motor e sistema de freios.",
            85000, null, null, new BigDecimal("350.00"), null, null
        );

        when(manutencaoService.criar(any())).thenReturn(osResponse());

        mockMvc.perform(post("/api/v1/manutencoes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.veiculoPlaca").value("ABC1234"))
            .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornar422QuandoDescricaoCurta() throws Exception {
        ManutencaoRequest request = new ManutencaoRequest(
            UUID.randomUUID(), UUID.randomUUID(), "Curta", null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/manutencoes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields.descricao").exists());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveListarOs() throws Exception {
        when(manutencaoService.listar(any(), any())).thenReturn(new PageImpl<>(List.of(osResponse())));

        mockMvc.perform(get("/api/v1/manutencoes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].veiculoPlaca").value("ABC1234"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveBuscarOsPorId() throws Exception {
        UUID id = UUID.randomUUID();
        when(manutencaoService.buscarPorId(id)).thenReturn(osResponse());

        mockMvc.perform(get("/api/v1/manutencoes/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mecanicoNome").value("Carlos Mecânico"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveAlterarStatus() throws Exception {
        UUID id = UUID.randomUUID();
        ManutencaoResponse concluida = new ManutencaoResponse(
            id, UUID.randomUUID(), UUID.randomUUID(), "XYZ9999", "Fiat", "Uno", null,
            "Maria", null, UUID.randomUUID(), "Carlos",
            "Revisão completa feita.", 50000, 50500,
            "Motor revisado com sucesso.", null,
            null, new BigDecimal("400.00"),
            StatusManutencao.CONCLUIDA,
            Instant.now(), Instant.now(), Instant.now(), Instant.now()
        );

        when(manutencaoService.alterarStatus(eq(id), any())).thenReturn(concluida);

        StatusUpdateRequest req = new StatusUpdateRequest(StatusManutencao.CONCLUIDA);

        mockMvc.perform(patch("/api/v1/manutencoes/{id}/status", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornarDadosParaImpressao() throws Exception {
        UUID id = UUID.randomUUID();
        when(manutencaoService.buscarPorId(id)).thenReturn(osResponse());
        when(empresaConfigService.buscar()).thenReturn(
            new EmpresaConfigResponse(UUID.randomUUID(), "Auto Center", "12.345.678/0001-99",
                "Rua A, 1", "(11) 0000-0000", "a@b.com", null)
        );

        mockMvc.perform(get("/api/v1/manutencoes/{id}/imprimir", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordemDeServico.veiculoPlaca").value("ABC1234"))
            .andExpect(jsonPath("$.empresa.nome").value("Auto Center"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornar403AoDeletarSemPermissao() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/manutencoes/{id}", id).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveDeletarComoAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/manutencoes/{id}", id).with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/manutencoes"))
            .andExpect(status().isUnauthorized());
    }

    private <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
