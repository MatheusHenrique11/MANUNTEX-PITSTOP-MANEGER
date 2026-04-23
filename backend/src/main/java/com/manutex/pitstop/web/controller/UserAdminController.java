package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.service.UserAdminService;
import com.manutex.pitstop.web.dto.UserRequest;
import com.manutex.pitstop.web.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> listar(Authentication auth) {
        return ResponseEntity.ok(userAdminService.listar(auth));
    }

    @PostMapping
    public ResponseEntity<UserResponse> criar(@Valid @RequestBody UserRequest request, Authentication auth) {
        UserResponse created = userAdminService.criar(request, auth);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> alterarStatus(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(userAdminService.alterarStatus(id, enabled));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> alterarRole(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userAdminService.alterarRole(id, request));
    }
}
