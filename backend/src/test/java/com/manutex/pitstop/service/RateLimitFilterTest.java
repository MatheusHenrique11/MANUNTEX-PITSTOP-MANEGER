package com.manutex.pitstop.service;

import com.manutex.pitstop.web.filter.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(3, 3, 60);
    }

    private void callFilter(String ip, MockHttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(ip);
        filter.doFilter(req, resp, chain);
    }

    @Test
    void devePermitirRequisicoesDentroDoLimite() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        callFilter("192.168.1.1", resp, chain);

        assertThat(resp.getStatus()).isNotEqualTo(429);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void deveAdicionarHeaderRateLimitRemaining() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        callFilter("10.0.0.1", resp, chain);

        assertThat(resp.getHeader("X-RateLimit-Remaining")).isNotNull();
    }

    @Test
    void deveBloquearAposExcederLimite() throws Exception {
        String ip = "192.168.99.99";

        // Consome todos os tokens
        for (int i = 0; i < 3; i++) {
            callFilter(ip, new MockHttpServletResponse(), mock(FilterChain.class));
        }

        // Próxima deve ser bloqueada
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);
        callFilter(ip, blockedResp, blockedChain);

        assertThat(blockedResp.getStatus()).isEqualTo(429);
        verify(blockedChain, never()).doFilter(any(), any());
    }

    @Test
    void deveTerBucketsSeparadosPorIp() throws Exception {
        // IP A consome todos os tokens
        for (int i = 0; i < 3; i++) {
            callFilter("10.0.0.100", new MockHttpServletResponse(), mock(FilterChain.class));
        }

        // IP B ainda deve ter tokens disponíveis
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chainB = mock(FilterChain.class);
        callFilter("10.0.0.200", resp, chainB);

        assertThat(resp.getStatus()).isNotEqualTo(429);
        verify(chainB).doFilter(any(), any());
    }

    @Test
    void deveUsarIpRealQuandoHaXForwardedFor() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.5, 192.168.1.1");
        req.setRemoteAddr("192.168.1.1");

        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isNotEqualTo(429);
        verify(chain).doFilter(req, resp);
    }
}
