package com.manutex.pitstop.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manutex.pitstop.config.SecurityConfig;
import com.manutex.pitstop.config.TestSecurityConfig;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.service.EmpresaConfigService;
import com.manutex.pitstop.web.dto.EmpresaConfigRequest;
import com.manutex.pitstop.web.dto.EmpresaConfigResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = EmpresaConfigController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@Import(TestSecurityConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class EmpresaConfigControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

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

    private EmpresaConfigResponse configResponse() {
        return new EmpresaConfigResponse(
            UUID.randomUUID(), "Auto Center Pitstop", "12.345.678/0001-99",
            "Rua das Flores, 123", "(11) 3333-4444", "contato@pitstop.com",
            "https://s3.example.com/logo.png"
        );
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveBuscarConfigDaEmpresa() throws Exception {
        when(empresaConfigService.buscar()).thenReturn(configResponse());

        mockMvc.perform(get("/api/v1/empresa"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Auto Center Pitstop"))
            .andExpect(jsonPath("$.cnpj").value("12.345.678/0001-99"))
            .andExpect(jsonPath("$.logoUrl").value("https://s3.example.com/logo.png"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveSalvarConfigComoAdmin() throws Exception {
        EmpresaConfigRequest request = new EmpresaConfigRequest(
            "Nova Oficina", "98.765.432/0001-00", "Av. Brasil, 500", "(21) 5555-6666", "nova@oficina.com"
        );

        when(empresaConfigService.salvar(any())).thenReturn(configResponse());

        mockMvc.perform(put("/api/v1/empresa")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Auto Center Pitstop"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornar403AoSalvarSemPermissao() throws Exception {
        EmpresaConfigRequest request = new EmpresaConfigRequest("Oficina", null, null, null, null);

        mockMvc.perform(put("/api/v1/empresa")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar422QuandoNomeDaEmpresaVazio() throws Exception {
        EmpresaConfigRequest request = new EmpresaConfigRequest("", null, null, null, null);

        mockMvc.perform(put("/api/v1/empresa")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields.nome").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveUploadLogoComoAdmin() throws Exception {
        MockMultipartFile logo = new MockMultipartFile(
            "file", "logo.png", "image/png", new byte[]{1, 2, 3, 4}
        );

        when(empresaConfigService.uploadLogo(any())).thenReturn(configResponse());

        mockMvc.perform(multipart("/api/v1/empresa/logo")
                .file(logo)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logoUrl").exists());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornar403AoUploadLogoSemPermissao() throws Exception {
        MockMultipartFile logo = new MockMultipartFile(
            "file", "logo.png", "image/png", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/empresa/logo")
                .file(logo)
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/empresa"))
            .andExpect(status().isUnauthorized());
    }
}
