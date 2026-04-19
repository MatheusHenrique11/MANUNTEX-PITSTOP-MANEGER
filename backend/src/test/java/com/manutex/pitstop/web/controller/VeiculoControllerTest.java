package com.manutex.pitstop.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manutex.pitstop.config.AppFeatures;
import com.manutex.pitstop.domain.entity.Cliente;
import com.manutex.pitstop.domain.entity.Veiculo;
import com.manutex.pitstop.domain.repository.ClienteRepository;
import com.manutex.pitstop.domain.repository.VeiculoRepository;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.web.dto.VeiculoRequest;
import com.manutex.pitstop.web.filter.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VeiculoController.class)
@MockBean(JpaMetamodelMappingContext.class)
class VeiculoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean VeiculoRepository veiculoRepository;
    @MockBean ClienteRepository clienteRepository;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean RateLimitFilter rateLimitFilter;

    private Veiculo veiculoMock() {
        return Veiculo.builder()
            .placa("ABC1234")
            .chassi("9BWZZZ377VT004251")
            .renavam("00258665590")
            .marca("Volkswagen")
            .modelo("Gol")
            .anoFabricacao(2020)
            .anoModelo(2021)
            .cor("Prata")
            .cliente(new Cliente())
            .build();
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveListarVeiculosComMasking() throws Exception {
        try (MockedStatic<AppFeatures> featuresMock = Mockito.mockStatic(AppFeatures.class)) {
            featuresMock.when(AppFeatures::values).thenReturn(AppFeatures.values());

            Veiculo v = veiculoMock();
            when(veiculoRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

            mockMvc.perform(get("/api/v1/veiculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].placa").value("ABC1234"))
                // Chassis deve estar mascarado para MECANICO
                .andExpect(jsonPath("$.content[0].chassi").value(org.hamcrest.Matchers.startsWith("*")));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveExporChassiCompletoParaAdmin() throws Exception {
        try (MockedStatic<AppFeatures> featuresMock = Mockito.mockStatic(AppFeatures.class)) {
            featuresMock.when(AppFeatures::values).thenReturn(AppFeatures.values());

            Veiculo v = veiculoMock();
            when(veiculoRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

            mockMvc.perform(get("/api/v1/veiculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].chassi").value("9BWZZZ377VT004251"));
        }
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/veiculos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarVeiculoComDadosValidos() throws Exception {
        UUID clienteId = UUID.randomUUID();
        VeiculoRequest request = new VeiculoRequest(
            "ABC1234", "9BWZZZ377VT004251", "00258665590",
            "Volkswagen", "Gol", 2020, 2021, "Prata", clienteId
        );

        Cliente cliente = new Cliente();
        Veiculo veiculo = veiculoMock();

        try (MockedStatic<AppFeatures> featuresMock = Mockito.mockStatic(AppFeatures.class)) {
            featuresMock.when(AppFeatures::values).thenReturn(AppFeatures.values());

            when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
            when(veiculoRepository.save(any())).thenReturn(veiculo);

            mockMvc.perform(post("/api/v1/veiculos")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("ABC1234"));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar422ParaPlacaInvalida() throws Exception {
        UUID clienteId = UUID.randomUUID();
        VeiculoRequest request = new VeiculoRequest(
            "INVALIDA!", "9BWZZZ377VT004251", "00258665590",
            "VW", "Gol", 2020, 2021, "Prata", clienteId
        );

        mockMvc.perform(post("/api/v1/veiculos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornar403AoTentarDeletarSemPermissao() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/veiculos/" + id).with(csrf()))
            .andExpect(status().isForbidden());
    }
}
