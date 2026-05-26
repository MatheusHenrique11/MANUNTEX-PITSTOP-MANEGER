package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.web.dto.EmpresaResponse;
import com.manutex.pitstop.web.dto.TenantRegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @SuppressWarnings("null") // JpaRepository.save() return is @NonNull by contract; false positive from Eclipse null checker
    public EmpresaResponse registrar(TenantRegistrationRequest req) {
        if (empresaRepository.existsByCnpj(req.cnpj())) {
            throw new EmpresaService.CnpjJaCadastradoException("CNPJ já cadastrado: " + req.cnpj());
        }
        if (userRepository.existsByEmail(req.gerenteEmail())) {
            throw new UserAdminService.EmailJaCadastradoException("E-mail já cadastrado: " + req.gerenteEmail());
        }

        Empresa empresa = empresaRepository.save(
            Empresa.builder()
                .nome(req.razaoSocial())
                .cnpj(formatCnpj(req.cnpj()))
                .razaoSocial(req.razaoSocial())
                .nomeFantasia(req.nomeFantasia())
                .cep(req.cep() != null ? req.cep().replaceAll("[^0-9]", "") : null)
                .logradouro(req.logradouro())
                .numero(req.numero())
                .complemento(req.complemento())
                .bairro(req.bairro())
                .cidade(req.cidade())
                .uf(req.uf())
                .codigoMunicipioIbge(req.codigoMunicipioIbge())
                .emailFiscal(req.emailFiscal())
                .telefoneFiscal(req.telefoneFiscal() != null ? req.telefoneFiscal().replaceAll("[^0-9]", "") : null)
                .ativo(true)
                .build()
        );

        userRepository.save(
            User.builder()
                .email(req.gerenteEmail())
                .passwordHash(passwordEncoder.encode(req.gerenteSenha()))
                .fullName(req.gerenteNome())
                .role(UserRole.ROLE_GERENTE)
                .enabled(true)
                .empresa(empresa)
                .build()
        );

        log.info("Novo tenant registrado: empresa={} CNPJ={} gerente={}", empresa.getId(), empresa.getCnpj(), req.gerenteEmail());
        return EmpresaResponse.of(empresa);
    }

    private String formatCnpj(String cnpj) {
        String digits = cnpj.replaceAll("[^0-9]", "");
        if (digits.length() == 14) {
            return digits.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return cnpj;
    }
}
