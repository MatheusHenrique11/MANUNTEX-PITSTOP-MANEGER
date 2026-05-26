package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByEmpresaId(UUID empresaId);
    long countByEmpresaIdAndRole(UUID empresaId, UserRole role);

    // Bypass @SQLRestriction to find/purge soft-deleted (anonymized) records
    @Modifying
    @Query(value = "DELETE FROM users WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff", nativeQuery = true)
    int hardDeleteAnonymized(@Param("cutoff") Instant cutoff);
}
