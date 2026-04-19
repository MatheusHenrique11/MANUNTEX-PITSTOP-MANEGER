package com.manutex.pitstop.web.exception;

import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.service.AesEncryptionService;
import com.manutex.pitstop.service.DocumentoService;
import com.manutex.pitstop.service.PdfMagicNumberValidator;
import com.manutex.pitstop.web.controller.AuthController;
import com.manutex.pitstop.service.AuthService;
import com.manutex.pitstop.web.filter.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@MockBean(JpaMetamodelMappingContext.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean RateLimitFilter rateLimitFilter;

    @Test
    void deveRetornarProblemDetailParaCredenciaisInvalidas() throws Exception {
        when(authService.login(any()))
            .thenThrow(new AuthService.InvalidCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@test.com\",\"password\":\"senha1234\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.title").value("Não autorizado"))
            .andExpect(jsonPath("$.detail").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deveRetornarErrosDeValidacaoComCampos() throws Exception {
        // Email inválido dispara MethodArgumentNotValidException
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nao_e_email\",\"password\":\"senha1234\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Dados inválidos"))
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void deveRetornarErroDeIntegridadeDocumento() throws Exception {
        when(authService.login(any()))
            .thenThrow(new DocumentoService.DocumentoIntegrityException("Checksum falhou"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@test.com\",\"password\":\"senha1234\"}"))
            .andExpect(status().isInternalServerError())
            // Mensagem interna NÃO deve vazar para o cliente
            .andExpect(jsonPath("$.detail").value("Não foi possível processar o documento. Contate o suporte."));
    }

    @Test
    void naoDeveExporStackTraceNaResposta() throws Exception {
        when(authService.login(any()))
            .thenThrow(new RuntimeException("detalhe interno sensível"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@test.com\",\"password\":\"senha1234\"}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente."))
            // "detalhe interno sensível" não deve aparecer
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("sensível"))));
    }
}
