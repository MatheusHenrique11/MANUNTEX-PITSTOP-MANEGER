package com.manutex.pitstop.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manutex.pitstop.domain.enums.FiscalEnvironment;
import com.manutex.pitstop.service.PlatformFiscalConfigService;
import com.manutex.pitstop.service.TenantFiscalConfigService;
import com.manutex.pitstop.web.dto.PlatformFiscalConfigRequest;
import com.manutex.pitstop.web.dto.PlatformFiscalConfigResponse;
import com.manutex.pitstop.web.dto.TenantFiscalConfigRequest;
import com.manutex.pitstop.web.dto.TenantFiscalConfigResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PlatformFiscalConfigController.class, TenantFiscalConfigController.class})
@SuppressWarnings("null") // MockMvc/csrf/MediaType: falsos positivos do checker com @NonNull
class FiscalControllerRbacTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PlatformFiscalConfigService platformService;
    @MockBean TenantFiscalConfigService   tenantService;

    // ── Platform Fiscal Config — ROLE_ADMIN ───────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeLerPlatformFiscalConfig() throws Exception {
        when(platformService.buscar()).thenReturn(Optional.of(fakePlatformResponse()));
        mockMvc.perform(get("/api/v1/admin/fiscal/platform"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoPodeAcessarPlatformFiscalConfig() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fiscal/platform"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void mecanicoNaoPodeAcessarPlatformFiscalConfig() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fiscal/platform"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeSalvarPlatformFiscalConfig() throws Exception {
        when(platformService.salvar(any())).thenReturn(fakePlatformResponse());

        mockMvc.perform(put("/api/v1/admin/fiscal/platform")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fakePlatformRequest())))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoPodeSalvarPlatformFiscalConfig() throws Exception {
        mockMvc.perform(put("/api/v1/admin/fiscal/platform")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fakePlatformRequest())))
            .andExpect(status().isForbidden());
    }

    // ── Tenant Fiscal Config — ROLE_GERENTE ───────────────────────────────────

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePodeLerTenantFiscalConfig() throws Exception {
        when(tenantService.buscar(any())).thenReturn(Optional.of(fakeTenantResponse()));
        mockMvc.perform(get("/api/v1/fiscal/tenant"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeLerTenantFiscalConfig() throws Exception {
        when(tenantService.buscar(any())).thenReturn(Optional.of(fakeTenantResponse()));
        mockMvc.perform(get("/api/v1/fiscal/tenant"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void mecanicoNaoPodeAcessarTenantFiscalConfig() throws Exception {
        mockMvc.perform(get("/api/v1/fiscal/tenant"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePodeSalvarTenantFiscalConfig() throws Exception {
        when(tenantService.salvar(any(), any())).thenReturn(fakeTenantResponse());

        mockMvc.perform(put("/api/v1/fiscal/tenant")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fakeTenantRequest())))
            .andExpect(status().isOk());
    }

    // ── Admin acessa TenantFiscalConfig por empresaId ─────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeAcessarTenantFiscalConfigPorEmpresaId() throws Exception {
        when(tenantService.buscar(any())).thenReturn(Optional.of(fakeTenantResponse()));
        mockMvc.perform(get("/api/v1/admin/fiscal/tenant/" + UUID.randomUUID()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoPodeAcessarTenantFiscalConfigDeOutraEmpresaViaAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fiscal/tenant/" + UUID.randomUUID()))
            .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PlatformFiscalConfigResponse fakePlatformResponse() {
        return new PlatformFiscalConfigResponse(
            UUID.randomUUID(), "RiseCode Studio", null,
            "99.***.***/**66-**", "123456", "Simples",
            "3550308", "São Paulo", "SP",
            "Rua Teste", "1", "Centro", "01000000",
            "contato@risecodestudio.com.br", null,
            "14.01", "1.01", new BigDecimal("2.00"),
            FiscalEnvironment.HOMOLOGACAO, false, null, null
        );
    }

    private PlatformFiscalConfigRequest fakePlatformRequest() {
        return new PlatformFiscalConfigRequest(
            "RiseCode Studio", null, "99.888.777/0001-66",
            "123456", "Simples", "3550308", "São Paulo", "SP",
            "Rua Teste", "1", "Centro", "01000000",
            "contato@risecodestudio.com.br", null,
            "14.01", "1.01", new BigDecimal("2.00"),
            FiscalEnvironment.HOMOLOGACAO
        );
    }

    private TenantFiscalConfigResponse fakeTenantResponse() {
        return new TenantFiscalConfigResponse(
            UUID.randomUUID(), UUID.randomUUID(),
            "Oficina Teste", null, "11.***.***/**81-**",
            "654321", "Simples", "3550308", "São Paulo", "SP",
            "Av. Mecânica", "10", "Industrial", "01001000",
            "fiscal@oficina.com", null,
            "14.01", "1.01", new BigDecimal("2.00"),
            FiscalEnvironment.HOMOLOGACAO, false, false, null, null
        );
    }

    private TenantFiscalConfigRequest fakeTenantRequest() {
        return new TenantFiscalConfigRequest(
            "Oficina Teste", null, "11.222.333/0001-81",
            "654321", "Simples", "3550308", "São Paulo", "SP",
            "Av. Mecânica", "10", "Industrial", "01001000",
            "fiscal@oficina.com", null, "14.01", "1.01",
            new BigDecimal("2.00"), FiscalEnvironment.HOMOLOGACAO, false
        );
    }
}
