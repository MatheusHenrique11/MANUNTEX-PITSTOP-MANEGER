package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.web.dto.EmpresaRequest;
import com.manutex.pitstop.web.dto.EmpresaResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmpresaResponse criar(EmpresaRequest request) {
        if (empresaRepository.existsByCnpj(request.cnpj())) {
            throw new CnpjJaCadastradoException("CNPJ já cadastrado: " + request.cnpj());
        }
        if (userRepository.existsByEmail(request.gerenteEmail())) {
            throw new UserAdminService.EmailJaCadastradoException("E-mail já cadastrado: " + request.gerenteEmail());
        }

        Empresa empresa = empresaRepository.save(
            Empresa.builder()
                .nome(request.nome())
                .cnpj(request.cnpj())
                .ativo(true)
                .build()
        );

        userRepository.save(
            User.builder()
                .email(request.gerenteEmail())
                .passwordHash(passwordEncoder.encode(request.gerenteSenha()))
                .fullName(request.gerenteNome())
                .role(UserRole.ROLE_GERENTE)
                .enabled(true)
                .empresa(empresa)
                .build()
        );

        return EmpresaResponse.of(empresa);
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> listar() {
        return empresaRepository.findAll().stream().map(EmpresaResponse::of).toList();
    }

    @Transactional
    public EmpresaResponse alterarStatus(UUID id, boolean ativo) {
        Empresa empresa = empresaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada: " + id));
        empresa.setAtivo(ativo);
        return EmpresaResponse.of(empresaRepository.save(empresa));
    }

    public static class CnpjJaCadastradoException extends RuntimeException {
        public CnpjJaCadastradoException(String msg) { super(msg); }
    }
}
