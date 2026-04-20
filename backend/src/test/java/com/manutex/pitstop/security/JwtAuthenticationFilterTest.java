package com.manutex.pitstop.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtService jwtService;
    @Mock FilterChain filterChain;
    @Mock Claims claims;

    @InjectMocks
    JwtAuthenticationFilter filter;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarComTokenValido() throws Exception {
        when(jwtService.validateAndExtract(anyString())).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user@pitstop.com");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_ADMIN"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "valid.jwt.token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user@pitstop.com");
        assertThat(auth.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deveExtrairTokenDoHeaderAuthorization() throws Exception {
        when(jwtService.validateAndExtract(anyString())).thenReturn(claims);
        when(claims.getSubject()).thenReturn("api@client.com");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_GERENTE"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("api@client.com");
    }

    @Test
    void devePriorizar_Cookie_SobreHeader() throws Exception {
        when(jwtService.validateAndExtract("cookie.token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("cookie@user.com");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_MECANICO"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "cookie.token"));
        request.addHeader("Authorization", "Bearer header.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(jwtService).validateAndExtract("cookie.token");
        verify(jwtService, never()).validateAndExtract("header.token");
    }

    @Test
    void naoDeveAutenticarComTokenInvalido() throws Exception {
        when(jwtService.validateAndExtract(anyString()))
            .thenThrow(new InvalidTokenException("Token expirado"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "expired.token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deveProsseguirSemAutenticarQuandoNaoHaToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void deveAtribuirMultiplasRolesCorretamente() throws Exception {
        when(jwtService.validateAndExtract(anyString())).thenReturn(claims);
        when(claims.getSubject()).thenReturn("multi@pitstop.com");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_ADMIN", "ROLE_GERENTE"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "valid.token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
            .extracting("authority")
            .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_GERENTE");
    }
}
