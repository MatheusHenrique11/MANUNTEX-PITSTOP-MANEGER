package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.UserConsent;
import com.manutex.pitstop.domain.enums.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    List<UserConsent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserConsent> findTopByUserIdAndPolicyTypeAndAcceptedTrueOrderByCreatedAtDesc(
        UUID userId, ConsentType policyType);

    boolean existsByUserIdAndPolicyTypeAndPolicyVersionAndAcceptedTrue(
        UUID userId, ConsentType policyType, String policyVersion);
}
