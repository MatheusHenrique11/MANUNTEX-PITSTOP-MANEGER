package com.manutex.pitstop.web.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void deveConstruirComTodosOsCampos() {
        AuthResponse resp = new AuthResponse("token.jwt", 900L, "ROLE_ADMIN", "admin@pitstop.com");

        assertThat(resp.accessToken()).isEqualTo("token.jwt");
        assertThat(resp.expiresIn()).isEqualTo(900L);
        assertThat(resp.role()).isEqualTo("ROLE_ADMIN");
        assertThat(resp.email()).isEqualTo("admin@pitstop.com");
    }

    @Test
    void deveAceitarAccessTokenNuloNoBodyDaResposta() {
        AuthResponse resp = new AuthResponse(null, 900L, "ROLE_MECANICO", "mecanico@pitstop.com");

        assertThat(resp.accessToken()).isNull();
        assertThat(resp.role()).isEqualTo("ROLE_MECANICO");
        assertThat(resp.email()).isEqualTo("mecanico@pitstop.com");
    }

    @Test
    void deveAceitarEmailNuloParaTokenRefreshSemEmail() {
        AuthResponse resp = new AuthResponse(null, 900L, "ROLE_ADMIN", null);

        assertThat(resp.email()).isNull();
        assertThat(resp.role()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void deveSerIgualComMesmosValores() {
        AuthResponse r1 = new AuthResponse("tok", 300L, "ROLE_GERENTE", "g@pitstop.com");
        AuthResponse r2 = new AuthResponse("tok", 300L, "ROLE_GERENTE", "g@pitstop.com");

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void toStringDeveConterCamposPrincipais() {
        AuthResponse resp = new AuthResponse(null, 900L, "ROLE_ADMIN", "a@b.com");
        String str = resp.toString();

        assertThat(str).contains("ROLE_ADMIN");
        assertThat(str).contains("a@b.com");
        assertThat(str).contains("900");
    }
}
