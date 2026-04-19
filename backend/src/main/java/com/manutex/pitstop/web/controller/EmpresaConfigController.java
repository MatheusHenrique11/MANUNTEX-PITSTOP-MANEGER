package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.service.EmpresaConfigService;
import com.manutex.pitstop.web.dto.EmpresaConfigRequest;
import com.manutex.pitstop.web.dto.EmpresaConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/empresa")
@RequiredArgsConstructor
public class EmpresaConfigController {

    private final EmpresaConfigService empresaConfigService;

    @GetMapping
    public ResponseEntity<EmpresaConfigResponse> buscar() {
        return ResponseEntity.ok(empresaConfigService.buscar());
    }

    @PutMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<EmpresaConfigResponse> salvar(@Valid @RequestBody EmpresaConfigRequest request) {
        return ResponseEntity.ok(empresaConfigService.salvar(request));
    }

    @PostMapping("/logo")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<EmpresaConfigResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(empresaConfigService.uploadLogo(file));
    }
}
