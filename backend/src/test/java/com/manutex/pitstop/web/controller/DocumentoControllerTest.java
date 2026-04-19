package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.config.SecurityConfig;
import com.manutex.pitstop.config.TestSecurityConfig;
import com.manutex.pitstop.domain.enums.TipoDocumento;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.service.DocumentoService;
import com.manutex.pitstop.service.PdfMagicNumberValidator;
import com.manutex.pitstop.web.dto.DocumentoResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = DocumentoController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
@Import(TestSecurityConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
class DocumentoControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean DocumentoService documentoService;
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
    @WithMockUser(roles = "MECANICO")
    void deveRetornarMetadadosSemStorageKey() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentoResponse resp = new DocumentoResponse(
            docId, UUID.randomUUID(), null,
            TipoDocumento.CRLV, "crlv.pdf", 5000L,
            "abc123hash", Instant.now(), null, null
        );

        MockMultipartFile file = new MockMultipartFile(
            "file", "crlv.pdf", "application/pdf", "%PDF-1.4".getBytes()
        );

        when(documentoService.upload(any(), any(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(multipart("/api/v1/documentos")
                .file(file)
                .param("tipo", "CRLV")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.nomeOriginal").value("crlv.pdf"))
            // storageKey NUNCA deve aparecer na resposta
            .andExpect(jsonPath("$.storageKey").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveServirPdfComHeadersDeSeguranca() throws Exception {
        UUID docId = UUID.randomUUID();
        byte[] pdfBytes = "%PDF-1.4 conteudo".getBytes();

        when(documentoService.getDecryptedContent(docId)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/documentos/{id}/view", docId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"documento.pdf\""));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornar422ParaPdfInvalido() throws Exception {
        MockMultipartFile malware = new MockMultipartFile(
            "file", "malware.pdf", "application/pdf", new byte[]{0x4D, 0x5A}
        );

        when(documentoService.upload(any(), any(), any(), any(), any()))
            .thenThrow(new PdfMagicNumberValidator.InvalidDocumentException("Não é PDF válido"));

        mockMvc.perform(multipart("/api/v1/documentos")
                .file(malware)
                .param("tipo", "CRLV")
                .with(csrf()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Documento inválido"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deveRetornar403AoTentarDeletarSemAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/documentos/{id}", id).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/documentos/veiculo/" + UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }
}
