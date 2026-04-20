package com.manutex.pitstop.config;

import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    UserRepository userRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private DataInitializer build(boolean enabled, String email, String password) {
        return new DataInitializer(userRepository, encoder, enabled, email, password);
    }

    @Test
    void deveCriarUsuarioQuandoHabilitadoESeedEmailNaoExiste() throws Exception {
        when(userRepository.existsByEmail("seed@test.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataInitializer initializer = build(true, "seed@test.com", "SenhaForte123!");
        initializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User created = captor.getValue();
        assertThat(created.getEmail()).isEqualTo("seed@test.com");
        assertThat(created.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(created.isEnabled()).isTrue();
        assertThat(encoder.matches("SenhaForte123!", created.getPassword())).isTrue();
    }

    @Test
    void deveIgnorarCriacaoQuandoDesabilitado() throws Exception {
        DataInitializer initializer = build(false, "seed@test.com", "SenhaForte123!");
        initializer.run(new DefaultApplicationArguments());

        verifyNoInteractions(userRepository);
    }

    @Test
    void deveIgnorarCriacaoQuandoEmailJaExiste() throws Exception {
        when(userRepository.existsByEmail("seed@test.com")).thenReturn(true);

        DataInitializer initializer = build(true, "seed@test.com", "SenhaForte123!");
        initializer.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
    }

    @Test
    void deveIgnorarCriacaoQuandoSenhaEstaBranca() throws Exception {
        DataInitializer initializer = build(true, "seed@test.com", "");
        initializer.run(new DefaultApplicationArguments());

        verifyNoInteractions(userRepository);
    }

    @Test
    void deveCriarSenhaComBCrypt() throws Exception {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataInitializer initializer = build(true, "seed@test.com", "MinhaSenha@2024");
        initializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        String hash = captor.getValue().getPassword();
        assertThat(hash).startsWith("$2a$");
        assertThat(hash).hasSizeGreaterThan(50);
    }
}
