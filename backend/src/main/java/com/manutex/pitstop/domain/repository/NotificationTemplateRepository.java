package com.manutex.pitstop.domain.repository;

import com.manutex.pitstop.domain.entity.NotificationTemplate;
import com.manutex.pitstop.domain.enums.NotificationChannel;
import com.manutex.pitstop.domain.enums.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    List<NotificationTemplate> findByEmpresaIdOrderByEventoAscCanalAsc(UUID empresaId);

    Optional<NotificationTemplate> findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
        UUID empresaId, NotificationEvent evento, NotificationChannel canal);

    /** Usado por seedDefaultTemplates — detecta templates ativos E inativos. */
    boolean existsByEmpresaIdAndEventoAndCanal(
        UUID empresaId, NotificationEvent evento, NotificationChannel canal);

    void deleteByEmpresaId(UUID empresaId);
}
