package com.manutex.pitstop.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manutex.pitstop.config.SecurityConfig;
import com.manutex.pitstop.config.TestSecurityConfig;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.service.UserAdminService;
import com.manutex.pitstop.web.dto.UserRequest;
import com.manutex.pitstop.web.dto.UserResponse;
import com.manutex.pitstop.web.filter.RateLimitFilter;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = UserAdminController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@Import(TestSecurityConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class UserAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserAdminService userAdminService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean RateLimitFilter rateLimitFilter;

    private UserResponse sampleResponse;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        sampleResponse = new UserResponse(userId, "novo@pitstop.com", "Novo Usuário",
                UserRole.ROLE_MECANICO, true, Instant.now());

        doAnswer(inv -> { inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        doAnswer(inv -> { inv.<FilterChain>getArgument(2).doFilter(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(rateLimitFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarUsuarioComSucesso() throws Exception {
        UserRequest req = new UserRequest("novo@pitstop.com", "Senha@1234", "Novo Usuário", UserRole.ROLE_MECANICO);
        when(userAdminService.criar(any(), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.email").value("novo@pitstop.com"))
            .andExpect(jsonPath("$.role").value("ROLE_MECANICO"))
            .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar409QuandoEmailJaExiste() throws Exception {
        UserRequest req = new UserRequest("dup@pitstop.com", "Senha@1234", "Dup", UserRole.ROLE_MECANICO);
        when(userAdminService.criar(any(), any()))
            .thenThrow(new UserAdminService.EmailJaCadastradoException("E-mail já cadastrado: dup@pitstop.com"));

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("E-mail já cadastrado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar422ComEmailInvalido() throws Exception {
        UserRequest req = new UserRequest("nao_e_email", "Senha@1234", "Teste", UserRole.ROLE_MECANICO);

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar422ComSenhaCurta() throws Exception {
        UserRequest req = new UserRequest("ok@pitstop.com", "123", "Teste", UserRole.ROLE_MECANICO);

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornar403ParaNaoAdmin() throws Exception {
        UserRequest req = new UserRequest("x@x.com", "Senha@1234", "X", UserRole.ROLE_MECANICO);

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveListarTodosOsUsuarios() throws Exception {
        when(userAdminService.listar(any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("novo@pitstop.com"))
            .andExpect(jsonPath("$[0].role").value("ROLE_MECANICO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveAlterarStatusDoUsuario() throws Exception {
        UserResponse desativado = new UserResponse(userId, "novo@pitstop.com",
                "Novo Usuário", UserRole.ROLE_MECANICO, false, Instant.now());
        when(userAdminService.alterarStatus(eq(userId), eq(false))).thenReturn(desativado);

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/status")
                .param("enabled", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoAlterarStatusDeInexistente() throws Exception {
        UUID inexistente = UUID.randomUUID();
        when(userAdminService.alterarStatus(eq(inexistente), anyBoolean()))
            .thenThrow(new EntityNotFoundException("Usuário não encontrado: " + inexistente));

        mockMvc.perform(patch("/api/v1/admin/users/" + inexistente + "/status")
                .param("enabled", "false"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isUnauthorized());
    }
}
