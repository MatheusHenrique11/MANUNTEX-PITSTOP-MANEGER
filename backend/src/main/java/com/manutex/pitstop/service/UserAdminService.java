package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.web.dto.UserRequest;
import com.manutex.pitstop.web.dto.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> listar(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return userRepository.findAll().stream().map(UserResponse::of).toList();
        }
        UUID empresaId = TenantContext.requireEmpresaId();
        return userRepository.findByEmpresaId(empresaId).stream().map(UserResponse::of).toList();
    }

    @Transactional
    public UserResponse criar(UserRequest request, Authentication auth) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException("E-mail já cadastrado: " + request.email());
        }

        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Empresa empresa = null;
        if (!isAdmin) {
            // GERENTE só pode criar MECANICO ou RECEPCIONISTA
            if (request.role() == UserRole.ROLE_ADMIN || request.role() == UserRole.ROLE_GERENTE) {
                throw new RoleNaoPermitidaException("GERENTE não pode criar usuários com role: " + request.role());
            }
            UUID empresaId = TenantContext.requireEmpresaId();
            empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada"));
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .enabled(true)
                .empresa(empresa)
                .build();
        return UserResponse.of(userRepository.save(user));
    }

    @Transactional
    public UserResponse alterarStatus(UUID id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
        user.setEnabled(enabled);
        return UserResponse.of(userRepository.save(user));
    }

    @Transactional
    public UserResponse alterarRole(UUID id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
        user.setRole(request.role());
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        return UserResponse.of(userRepository.save(user));
    }

    public static class EmailJaCadastradoException extends RuntimeException {
        public EmailJaCadastradoException(String msg) { super(msg); }
    }

    public static class RoleNaoPermitidaException extends RuntimeException {
        public RoleNaoPermitidaException(String msg) { super(msg); }
    }
}
