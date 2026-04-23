package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Cliente;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.repository.ClienteRepository;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.web.dto.ClienteRequest;
import com.manutex.pitstop.web.dto.ClienteResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(String q, Pageable pageable, boolean exposeConfidential) {
        UUID empresaId = TenantContext.requireEmpresaId();
        Page<Cliente> page = (q != null && !q.isBlank())
            ? clienteRepository.searchByEmpresa(empresaId, q, pageable)
            : clienteRepository.findByEmpresaId(empresaId, pageable);
        return page.map(c -> ClienteResponse.of(c, exposeConfidential));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(UUID id, boolean exposeConfidential) {
        UUID empresaId = TenantContext.requireEmpresaId();
        Cliente cliente = clienteRepository.findById(id)
            .filter(c -> empresaId.equals(c.getEmpresa() != null ? c.getEmpresa().getId() : null))
            .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + id));
        return ClienteResponse.of(cliente, exposeConfidential);
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        UUID empresaId = TenantContext.requireEmpresaId();
        if (clienteRepository.existsByCpfCnpjAndEmpresaId(request.cpfCnpj(), empresaId)) {
            throw new CpfCnpjJaCadastradoException("CPF/CNPJ já cadastrado nesta empresa: " + request.cpfCnpj());
        }
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada"));

        Cliente cliente = clienteRepository.save(
            Cliente.builder()
                .nome(request.nome())
                .cpfCnpj(request.cpfCnpj())
                .telefone(request.telefone())
                .email(request.email())
                .empresa(empresa)
                .build()
        );
        return ClienteResponse.of(cliente, true);
    }

    public static class CpfCnpjJaCadastradoException extends RuntimeException {
        public CpfCnpjJaCadastradoException(String msg) { super(msg); }
    }
}
