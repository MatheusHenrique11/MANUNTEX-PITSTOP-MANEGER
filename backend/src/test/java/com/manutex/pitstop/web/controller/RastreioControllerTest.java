package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.config.SecurityConfig;
import com.manutex.pitstop.config.TestSecurityConfig;
import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.entity.Veiculo;
import com.manutex.pitstop.domain.enums.StatusManutencao;
import com.manutex.pitstop.domain.repository.ManutencaoRepository;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = RastreioController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@Import(TestSecurityConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class RastreioControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ManutencaoRepository manutencaoRepository;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean RateLimitFilter rateLimitFilter;

    @BeforeEach
    void configureMockFilters() throws Exception {
        doAnswer(inv -> { inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        doAnswer(inv -> { inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(rateLimitFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    // Cria mocks de entidade FORA do thenReturn() para evitar UnfinishedStubbingException
    private Manutencao buildManutencaoMock(UUID token) {
        Veiculo veiculo = mock(Veiculo.class);
        when(veiculo.getPlaca()).thenReturn("ABC1D23");
        when(veiculo.getMarca()).thenReturn("Toyota");
        when(veiculo.getModelo()).thenReturn("Corolla");
        when(veiculo.getCor()).thenReturn("Branco");

        User mecanico = mock(User.class);
        when(mecanico.getFullName()).thenReturn("Carlos Silva");

        Manutencao m = mock(Manutencao.class);
        when(m.getTrackingToken()).thenReturn(token);
        when(m.getStatus()).thenReturn(StatusManutencao.EM_ANDAMENTO);
        when(m.getVeiculo()).thenReturn(veiculo);
        when(m.getMecanico()).thenReturn(mecanico);
        when(m.getDescricao()).thenReturn("Troca de óleo e revisão dos freios dianteiros.");
        when(m.getObservacoes()).thenReturn("Desgaste no disco dianteiro.");
        when(m.getDataEntrada()).thenReturn(Instant.now());
        when(m.getDataConclusao()).thenReturn(null);
        return m;
    }

    @Test
    void deveRetornarDadosDaOsSemAutenticacao() throws Exception {
        UUID token = UUID.randomUUID();
        Manutencao m = buildManutencaoMock(token); // pré-computa ANTES do when()
        when(manutencaoRepository.findByTrackingToken(token)).thenReturn(Optional.of(m));

        // sem @WithMockUser — valida que o endpoint é público
        mockMvc.perform(get("/api/v1/public/rastreio/{token}", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
            .andExpect(jsonPath("$.veiculoPlaca").value("ABC1D23"))
            .andExpect(jsonPath("$.veiculoMarca").value("Toyota"))
            .andExpect(jsonPath("$.mecanicoNome").value("Carlos"))
            .andExpect(jsonPath("$.trackingToken").value(token.toString()));
    }

    @Test
    void deveRetornar404QuandoTokenInexistente() throws Exception {
        UUID token = UUID.randomUUID();
        when(manutencaoRepository.findByTrackingToken(token)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/public/rastreio/{token}", token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void deveRetornar400QuandoTokenNaoEhUUID() throws Exception {
        mockMvc.perform(get("/api/v1/public/rastreio/nao-e-um-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    @Test
    void naoDeveExporDadosSensiveisNaResposta() throws Exception {
        UUID token = UUID.randomUUID();
        Manutencao m = buildManutencaoMock(token); // pré-computa ANTES do when()
        when(manutencaoRepository.findByTrackingToken(token)).thenReturn(Optional.of(m));

        mockMvc.perform(get("/api/v1/public/rastreio/{token}", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcamento").doesNotExist())
            .andExpect(jsonPath("$.valorFinal").doesNotExist())
            .andExpect(jsonPath("$.clienteTelefone").doesNotExist())
            .andExpect(jsonPath("$.mecanicoId").doesNotExist());
    }
}
