package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.domain.enums.StatusManutencao;
import com.manutex.pitstop.service.EmpresaConfigService;
import com.manutex.pitstop.service.ManutencaoService;
import com.manutex.pitstop.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manutencoes")
@RequiredArgsConstructor
public class ManutencaoController {

    private final ManutencaoService manutencaoService;
    private final EmpresaConfigService empresaConfigService;

    @PostMapping
    public ResponseEntity<ManutencaoResponse> criar(@Valid @RequestBody ManutencaoRequest request) {
        return ResponseEntity.ok(manutencaoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<Page<ManutencaoResponse>> listar(
        @RequestParam(required = false) StatusManutencao status,
        Pageable pageable
    ) {
        return ResponseEntity.ok(manutencaoService.listar(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManutencaoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(manutencaoService.buscarPorId(id));
    }

    @GetMapping("/veiculo/{veiculoId}")
    public ResponseEntity<Page<ManutencaoResponse>> listarPorVeiculo(
        @PathVariable UUID veiculoId,
        @RequestParam(required = false) StatusManutencao status,
        Pageable pageable
    ) {
        return ResponseEntity.ok(manutencaoService.listarPorVeiculo(veiculoId, status, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManutencaoResponse> atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody ManutencaoUpdateRequest request
    ) {
        return ResponseEntity.ok(manutencaoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ManutencaoResponse> alterarStatus(
        @PathVariable UUID id,
        @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(manutencaoService.alterarStatus(id, request.status()));
    }

    @GetMapping("/{id}/imprimir")
    public ResponseEntity<ManutencaoPrintResponse> imprimir(@PathVariable UUID id) {
        ManutencaoResponse os = manutencaoService.buscarPorId(id);
        EmpresaConfigResponse empresa = empresaConfigService.buscar();
        return ResponseEntity.ok(new ManutencaoPrintResponse(os, empresa));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        manutencaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
