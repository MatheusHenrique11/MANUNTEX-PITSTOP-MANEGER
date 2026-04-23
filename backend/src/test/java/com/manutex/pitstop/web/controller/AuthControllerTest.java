package com.manutex.pitstop.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manutex.pitstop.config.SecurityConfig;
import com.manutex.pitstop.config.TestSecurityConfig;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.service.AuthService;
import com.manutex.pitstop.web.dto.AuthRequest;
import com.manutex.pitstop.web.dto.AuthResponse;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@Import(TestSecurityConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean RateLimitFilter rateLimitFilter;

    @BeforeEach
    void configureMockFilters() throws Exception {
        doAnswer(inv -> { inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        doAnswer(inv -> { inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(rateLimitFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void deveRetornarAuthResponseNoLogin() throws Exception {
        AuthRequest req = new AuthRequest("admin@test.com", "senha1234");
        AuthResponse resp = new AuthResponse("jwt.access.token", 900L, "ROLE_ADMIN", "admin@test.com", null);

        when(authService.login(any())).thenReturn(resp);
        when(authService.loginAndGetRefreshToken(any())).thenReturn("raw_refresh_token");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            // Access token NÃO deve estar no body (está no cookie HTTP-Only)
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
            .andExpect(jsonPath("$.email").value("admin@test.com"))
            // Cookies HTTP-Only devem estar presentes
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().httpOnly("access_token", true))
            .andExpect(cookie().exists("refresh_token"))
            .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void deveRetornar401ComCredenciaisInvalidas() throws Exception {
        AuthRequest req = new AuthRequest("user@test.com", "errada1234");

        when(authService.login(any()))
            .thenThrow(new AuthService.InvalidCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Não autorizado"));
    }

    @Test
    void deveRetornar422ComEmailInvalido() throws Exception {
        AuthRequest req = new AuthRequest("nao_e_email", "senha1234");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void deveRetornar422ComSenhaCurta() throws Exception {
        AuthRequest req = new AuthRequest("user@test.com", "123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void deveApagarCookiesNoLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("access_token", 0))
            .andExpect(cookie().maxAge("refresh_token", 0));
    }
}
