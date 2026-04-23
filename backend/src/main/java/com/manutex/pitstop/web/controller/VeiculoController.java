package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.config.AppFeatures;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.Veiculo;
import com.manutex.pitstop.domain.repository.ClienteRepository;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.VeiculoRepository;
import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.web.dto.VeiculoRequest;
import com.manutex.pitstop.web.dto.VeiculoResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;

    @GetMapping
    public ResponseEntity<Page<VeiculoResponse>> listar(
        @RequestParam(required = false) String q,
        Pageable pageable,
        Authentication auth
    ) {
        if (!AppFeatures.VEHICLE_MANAGEMENT.isActive()) {
            return ResponseEntity.status(503).build();
        }

        boolean exposeConfidential = hasPrivilegedRole(auth);
        boolean isAdmin = isAdmin(auth);

        Page<Veiculo> page;
        if (isAdmin) {
            page = (q != null && !q.isBlank())
                ? veiculoRepository.search(q, pageable)
                : veiculoRepository.findAll(pageable);
        } else {
            UUID empresaId = TenantContext.requireEmpresaId();
            page = (q != null && !q.isBlank())
                ? veiculoRepository.searchByEmpresa(empresaId, q, pageable)
                : veiculoRepository.findByClienteEmpresaId(empresaId, pageable);
        }

        return ResponseEntity.ok(page.map(v -> VeiculoResponse.of(v, exposeConfidential)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponse> buscarPorId(@PathVariable UUID id, Authentication auth) {
        Veiculo v = veiculoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado: " + id));
        return ResponseEntity.ok(VeiculoResponse.of(v, hasPrivilegedRole(auth)));
    }

    @PostMapping
    public ResponseEntity<VeiculoResponse> criar(@Valid @RequestBody VeiculoRequest request, Authentication auth) {
        if (!AppFeatures.VEHICLE_MANAGEMENT.isActive()) {
            return ResponseEntity.status(503).build();
        }

        Optional<UUID> empresaId = TenantContext.currentEmpresaId();
        var cliente = clienteRepository.findById(request.clienteId())
            .filter(c -> isAdmin(auth) || empresaId.map(eid -> eid.equals(c.getEmpresa() != null ? c.getEmpresa().getId() : null)).orElse(false))
            .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + request.clienteId()));

        Veiculo veiculo = Veiculo.builder()
            .placa(request.placa())
            .chassi(request.chassi())
            .renavam(request.renavam())
            .marca(request.marca())
            .modelo(request.modelo())
            .anoFabricacao(request.anoFabricacao())
            .anoModelo(request.anoModelo())
            .cor(request.cor())
            .cliente(cliente)
            .build();

        Veiculo saved = veiculoRepository.save(veiculo);
        return ResponseEntity.ok(VeiculoResponse.of(saved, hasPrivilegedRole(auth)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        veiculoRepository.findById(id).ifPresent(veiculoRepository::delete);
        return ResponseEntity.noContent().build();
    }

    private boolean hasPrivilegedRole(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                       || a.getAuthority().equals("ROLE_GERENTE"));
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
