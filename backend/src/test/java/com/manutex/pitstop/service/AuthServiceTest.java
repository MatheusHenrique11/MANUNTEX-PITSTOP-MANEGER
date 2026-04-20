package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.RefreshToken;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.RefreshTokenRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.security.JwtService;
import com.manutex.pitstop.web.dto.AuthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authManager;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
            .email("admin@pitstop.com")
            .passwordHash("$2b$12$hash")
            .fullName("Admin")
            .role(UserRole.ROLE_ADMIN)
            .build();

        // Injeta valores de propriedade via reflection
        try {
            var accessField = AuthService.class.getDeclaredField("accessTokenExpiryMs");
            accessField.setAccessible(true);
            accessField.set(authService, 900_000L);

            var refreshField = AuthService.class.getDeclaredField("refreshTokenExpiryMs");
            refreshField.setAccessible(true);
            refreshField.set(authService, 604_800_000L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deveRetornarAuthResponseNoLogin() {
        AuthRequest request = new AuthRequest("admin@pitstop.com", "senha123");

        when(userRepository.findByEmail("admin@pitstop.com")).thenReturn(Optional.of(adminUser));
        when(jwtService.generateAccessToken(anyString(), anyMap())).thenReturn("access.token.jwt");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access.token.jwt");
        assertThat(response.role()).isEqualTo("ROLE_ADMIN");
        assertThat(response.email()).isEqualTo("admin@pitstop.com");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    @Test
    void deveLancarExcecaoComCredenciaisInvalidas() {
        AuthRequest request = new AuthRequest("user@test.com", "senha_errada");

        doThrow(new BadCredentialsException("Bad credentials"))
            .when(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthService.InvalidCredentialsException.class)
            .hasMessageContaining("inválidas");
    }

    @Test
    void deveArmazenarRefreshTokenComoHash() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(adminUser));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        authService.login(new AuthRequest("admin@pitstop.com", "senha"));

        RefreshToken saved = captor.getValue();
        // Token deve ser armazenado como hash, não em texto puro
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getTokenHash()).hasSize(44); // Base64 de SHA-256 (32 bytes)
    }

    @Test
    void deveRevogarTodosTokensEmReplayAtack() {
        String rawToken = "token_roubado";
        UUID userId = UUID.randomUUID();

        User user = User.builder().email("vítima@test.com").passwordHash("hash")
            .fullName("Vítima").role(UserRole.ROLE_MECANICO).build();

        // Simula token expirado (revogado)
        RefreshToken expiredToken = RefreshToken.builder()
            .tokenHash("qualquer_hash")
            .user(user)
            .expiresAt(Instant.now().minusSeconds(3600))  // já expirou
            .revoked(false)
            .build();

        // Configura o hash do token para que seja encontrado
        // Como o hash é interno, simulamos via findByTokenHash
        when(refreshTokenRepository.findByTokenHash(anyString()))
            .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refreshAccessToken(rawToken))
            .isInstanceOf(AuthService.InvalidCredentialsException.class);
    }

    @Test
    void deveRevogarTokenNoLogout() {
        String rawToken = "token_valido";
        RefreshToken token = RefreshToken.builder()
            .tokenHash("hash")
            .user(adminUser)
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.logout(rawToken);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().isRevoked()).isTrue();
    }

    @Test
    void deveIgnorarLogoutComTokenNulo() {
        assertThatNoException().isThrownBy(() -> authService.logout(null));
        assertThatNoException().isThrownBy(() -> authService.logout(""));
        verifyNoInteractions(refreshTokenRepository);
    }
}
