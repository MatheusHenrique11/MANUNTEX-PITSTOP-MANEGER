package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.web.dto.UserRequest;
import com.manutex.pitstop.web.dto.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserAdminService service;

    private User adminUser;
    private UUID adminId;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        adminUser = User.builder()
                .email("admin@pitstop.com")
                .passwordHash("$2a$hash")
                .fullName("Admin Teste")
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .build();

        adminAuth = mock(Authentication.class);
        lenient().when(adminAuth.getAuthorities()).thenAnswer(inv ->
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void deveCriarUsuarioComSenhaHasheada() {
        UserRequest req = new UserRequest("novo@pitstop.com", "Senha@1234", "Novo Usuário", UserRole.ROLE_MECANICO);

        when(userRepository.existsByEmail("novo@pitstop.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha@1234")).thenReturn("$2a$hash_encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.criar(req, adminAuth);

        assertThat(resp.email()).isEqualTo("novo@pitstop.com");
        assertThat(resp.fullName()).isEqualTo("Novo Usuário");
        assertThat(resp.role()).isEqualTo(UserRole.ROLE_MECANICO);
        assertThat(resp.enabled()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$hash_encoded");
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaExiste() {
        UserRequest req = new UserRequest("admin@pitstop.com", "Senha@1234", "Outro", UserRole.ROLE_GERENTE);
        when(userRepository.existsByEmail("admin@pitstop.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(req, adminAuth))
                .isInstanceOf(UserAdminService.EmailJaCadastradoException.class)
                .hasMessageContaining("admin@pitstop.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void deveListarTodosOsUsuariosParaAdmin() {
        User mecanico = User.builder()
                .email("mec@pitstop.com").passwordHash("hash")
                .fullName("Mecânico").role(UserRole.ROLE_MECANICO).enabled(true).build();

        when(userRepository.findAll()).thenReturn(List.of(adminUser, mecanico));

        List<UserResponse> lista = service.listar(adminAuth);

        assertThat(lista).hasSize(2);
        assertThat(lista).extracting(UserResponse::email)
                .containsExactly("admin@pitstop.com", "mec@pitstop.com");
    }

    @Test
    void deveDesativarUsuario() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.alterarStatus(adminId, false);

        assertThat(resp.enabled()).isFalse();
    }

    @Test
    void deveReativarUsuario() {
        adminUser.setEnabled(false);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.alterarStatus(adminId, true);

        assertThat(resp.enabled()).isTrue();
    }

    @Test
    void deveLancarExcecaoAoAlterarStatusDeUsuarioInexistente() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterarStatus(id, false))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deveAlterarRoleDoUsuario() {
        UserRequest req = new UserRequest("admin@pitstop.com", "qualquer", "Admin Teste", UserRole.ROLE_GERENTE);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = service.alterarRole(adminId, req);

        assertThat(resp.role()).isEqualTo(UserRole.ROLE_GERENTE);
    }

    @Test
    void deveLancarExcecaoAoAlterarRoleDeUsuarioInexistente() {
        UUID id = UUID.randomUUID();
        UserRequest req = new UserRequest("x@x.com", "p", "N", UserRole.ROLE_MECANICO);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterarRole(id, req))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
