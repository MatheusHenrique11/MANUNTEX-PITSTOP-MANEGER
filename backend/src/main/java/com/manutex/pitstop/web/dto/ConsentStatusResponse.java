package com.manutex.pitstop.web.dto;

import java.time.Instant;

/**
 * Estado atual do consentimento do usuário autenticado.
 * allRequired = true indica que o usuário pode usar o sistema normalmente.
 */
public record ConsentStatusResponse(
    String currentPrivacyPolicyVersion,
    boolean privacyPolicyAccepted,
    Instant privacyPolicyAcceptedAt,

    String currentTermsOfUseVersion,
    boolean termsOfUseAccepted,
    Instant termsOfUseAcceptedAt,

    boolean allRequired
) {}
