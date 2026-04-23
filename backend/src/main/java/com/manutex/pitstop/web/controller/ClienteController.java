package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.service.ClienteService;
import com.manutex.pitstop.web.dto.ClienteRequest;
import com.manutex.pitstop.web.dto.ClienteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<Page<ClienteResponse>> listar(
        @RequestParam(required = false) String q,
        Pageable pageable,
        Authentication auth
    ) {
        return ResponseEntity.ok(clienteService.listar(q, pageable, hasPrivilegedRole(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(clienteService.buscarPorId(id, hasPrivilegedRole(auth)));
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(
        @Valid @RequestBody ClienteRequest request,
        Authentication auth
    ) {
        return ResponseEntity.ok(clienteService.criar(request));
    }

    private boolean hasPrivilegedRole(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                       || a.getAuthority().equals("ROLE_GERENTE"));
    }
}
