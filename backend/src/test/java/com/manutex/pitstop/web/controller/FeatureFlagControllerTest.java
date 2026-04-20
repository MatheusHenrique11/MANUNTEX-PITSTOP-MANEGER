package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.config.AppFeatures;
import com.manutex.pitstop.config.SecurityConfig;
import com.manutex.pitstop.config.TestSecurityConfig;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.web.filter.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.togglz.core.Feature;
import org.togglz.core.context.FeatureContext;
import org.togglz.core.manager.FeatureManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = FeatureFlagController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@Import(TestSecurityConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class FeatureFlagControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean RateLimitFilter rateLimitFilter;

    @BeforeEach
    void configureMockFilters() throws Exception {
        doAnswer(inv -> {
            inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(inv -> {
            inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(rateLimitFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornarTodasAsFeaturesComStatusAtivo() throws Exception {
        FeatureManager mockManager = mock(FeatureManager.class);
        when(mockManager.isActive(any(Feature.class))).thenReturn(true);

        try (MockedStatic<FeatureContext> ctx = mockStatic(FeatureContext.class)) {
            ctx.when(FeatureContext::getFeatureManager).thenReturn(mockManager);

            mockMvc.perform(get("/api/v1/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.VEHICLE_MANAGEMENT").exists())
                .andExpect(jsonPath("$.VEHICLE_MANAGEMENT.active").value(true))
                .andExpect(jsonPath("$.VEHICLE_MANAGEMENT.label").isString())
                .andExpect(jsonPath("$.DOCUMENT_VAULT").exists())
                .andExpect(jsonPath("$.MAINTENANCE_MODULE").exists());
        }
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornarFeaturesDesativadasCorretamente() throws Exception {
        FeatureManager mockManager = mock(FeatureManager.class);
        when(mockManager.isActive(any(Feature.class))).thenReturn(false);

        try (MockedStatic<FeatureContext> ctx = mockStatic(FeatureContext.class)) {
            ctx.when(FeatureContext::getFeatureManager).thenReturn(mockManager);

            mockMvc.perform(get("/api/v1/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.VEHICLE_MANAGEMENT.active").value(false))
                .andExpect(jsonPath("$.DOCUMENT_VAULT.active").value(false));
        }
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/features"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornarTodasAsFeaturesDoEnum() throws Exception {
        FeatureManager mockManager = mock(FeatureManager.class);
        when(mockManager.isActive(any(Feature.class))).thenReturn(true);

        try (MockedStatic<FeatureContext> ctx = mockStatic(FeatureContext.class)) {
            ctx.when(FeatureContext::getFeatureManager).thenReturn(mockManager);

            mockMvc.perform(get("/api/v1/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());

            for (AppFeatures feature : AppFeatures.values()) {
                mockMvc.perform(get("/api/v1/features"))
                    .andExpect(jsonPath("$." + feature.name()).exists());
            }
        }
    }
}
