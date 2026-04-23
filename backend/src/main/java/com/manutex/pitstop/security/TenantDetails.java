package com.manutex.pitstop.security;

import java.util.UUID;

/**
 * Substitui WebAuthenticationDetails para carregar empresaId no contexto de segurança.
 * Obtido via SecurityContextHolder — elimina consultas extras ao banco por request.
 */
public record TenantDetails(String remoteAddress, UUID empresaId) {}
