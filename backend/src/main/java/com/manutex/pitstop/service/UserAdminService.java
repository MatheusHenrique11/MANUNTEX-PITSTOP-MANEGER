package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.web.dto.UserRequest;
import com.manutex.pitstop.web.dto.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> listarTodos() {
        return userRepository.findAll().stream()
                .map(UserResponse::of)
                .toList();
    }

    @Transactional
    public UserResponse criar(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException("E-mail já cadastrado: " + request.email());
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .enabled(true)
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
}
