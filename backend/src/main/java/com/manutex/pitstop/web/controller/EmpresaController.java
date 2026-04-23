package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.service.EmpresaService;
import com.manutex.pitstop.web.dto.EmpresaRequest;
import com.manutex.pitstop.web.dto.EmpresaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/empresas")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<EmpresaResponse> criar(@Valid @RequestBody EmpresaRequest request) {
        return ResponseEntity.ok(empresaService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listar() {
        return ResponseEntity.ok(empresaService.listar());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EmpresaResponse> alterarStatus(
        @PathVariable UUID id,
        @RequestParam boolean ativo
    ) {
        return ResponseEntity.ok(empresaService.alterarStatus(id, ativo));
    }
}
