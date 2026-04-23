package com.manutex.pitstop.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private TenantContext() {}

    public static Optional<UUID> currentEmpresaId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof TenantDetails td)) {
            return Optional.empty();
        }
        return Optional.ofNullable(td.empresaId());
    }

    public static UUID requireEmpresaId() {
        return currentEmpresaId()
            .orElseThrow(() -> new IllegalStateException("Nenhuma empresa no contexto de segurança"));
    }
}
