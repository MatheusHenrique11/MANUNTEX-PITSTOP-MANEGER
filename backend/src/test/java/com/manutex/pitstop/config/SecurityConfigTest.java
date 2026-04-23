package com.manutex.pitstop.config;

import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.security.JwtAuthenticationFilter;
import com.manutex.pitstop.web.filter.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock private RateLimitFilter rateLimitFilter;
    @Mock private UserRepository userRepository;

    private SecurityConfig securityConfig;

    private User testUser;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtAuthenticationFilter, rateLimitFilter, userRepository);

        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("mecanico@pitstop.com")
            .passwordHash("$2b$12$hashfake")
            .fullName("Mecânico Teste")
            .role(UserRole.ROLE_MECANICO)
            .build();
    }

    // ── UserDetailsService ────────────────────────────────────────────────────

    @Test
    void userDetailsServiceDeveCarregarUsuarioPorEmail() {
        when(userRepository.findByEmail("mecanico@pitstop.com")).thenReturn(Optional.of(testUser));

        var uds = securityConfig.userDetailsService();
        UserDetails result = uds.loadUserByUsername("mecanico@pitstop.com");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("mecanico@pitstop.com");
    }

    @Test
    void userDetailsServiceDeveRetornarPasswordHashCorreto() {
        when(userRepository.findByEmail("mecanico@pitstop.com")).thenReturn(Optional.of(testUser));

        var uds = securityConfig.userDetailsService();
        UserDetails result = uds.loadUserByUsername("mecanico@pitstop.com");

        assertThat(result.getPassword()).isEqualTo("$2b$12$hashfake");
    }

    @Test
    void userDetailsServiceDeveRetornarAuthorityCorreta() {
        when(userRepository.findByEmail("mecanico@pitstop.com")).thenReturn(Optional.of(testUser));

        var uds = securityConfig.userDetailsService();
        UserDetails result = uds.loadUserByUsername("mecanico@pitstop.com");

        assertThat(result.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_MECANICO");
    }

    @Test
    void userDetailsServiceDeveLancarUsernameNotFoundExceptionParaEmailDesconhecido() {
        when(userRepository.findByEmail("naoexiste@pitstop.com")).thenReturn(Optional.empty());

        var uds = securityConfig.userDetailsService();

        assertThatThrownBy(() -> uds.loadUserByUsername("naoexiste@pitstop.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("naoexiste@pitstop.com");
    }

    @Test
    void userDetailsServiceDeveChamarFindByEmailComEmailCorreto() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));

        var uds = securityConfig.userDetailsService();
        uds.loadUserByUsername("mecanico@pitstop.com");

        verify(userRepository).findByEmail("mecanico@pitstop.com");
    }

    // ── PasswordEncoder ───────────────────────────────────────────────────────

    @Test
    void passwordEncoderDeveSerBCryptPasswordEncoder() {
        assertThat(securityConfig.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoderDeveFazerHashCorretamente() {
        var encoder = securityConfig.passwordEncoder();
        String raw = "SenhaForte@2025";
        String hash = encoder.encode(raw);

        assertThat(hash).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, hash)).isTrue();
    }

    @Test
    void passwordEncoderNaoDeveAceitarSenhaErrada() {
        var encoder = securityConfig.passwordEncoder();
        String hash = encoder.encode("SenhaCorreta");

        assertThat(encoder.matches("SenhaErrada", hash)).isFalse();
    }

    @Test
    void passwordEncoderDeveProduzirHashesDiferentesParaMesmaSenha() {
        var encoder = securityConfig.passwordEncoder();
        String raw = "MesmaSenha";

        String hash1 = encoder.encode(raw);
        String hash2 = encoder.encode(raw);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(encoder.matches(raw, hash1)).isTrue();
        assertThat(encoder.matches(raw, hash2)).isTrue();
    }

    // ── AuthenticationProvider ────────────────────────────────────────────────

    @Test
    void authenticationProviderDeveSerDaoAuthenticationProvider() {
        assertThat(securityConfig.authenticationProvider())
            .isInstanceOf(DaoAuthenticationProvider.class);
    }

    @Test
    void authenticationProviderDeveAutenticarCredenciaisValidas() {
        when(userRepository.findByEmail("mecanico@pitstop.com")).thenReturn(Optional.of(testUser));

        var encoder = securityConfig.passwordEncoder();
        String senhaRaw = "SenhaValida@2025";

        User userComSenhaReal = User.builder()
            .id(UUID.randomUUID())
            .email("mecanico@pitstop.com")
            .passwordHash(encoder.encode(senhaRaw))
            .fullName("Mecânico")
            .role(UserRole.ROLE_MECANICO)
            .build();
        when(userRepository.findByEmail("mecanico@pitstop.com")).thenReturn(Optional.of(userComSenhaReal));

        var provider = (DaoAuthenticationProvider) securityConfig.authenticationProvider();
        var token = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "mecanico@pitstop.com", senhaRaw
        );

        assertThatNoException().isThrownBy(() -> provider.authenticate(token));
    }
}
