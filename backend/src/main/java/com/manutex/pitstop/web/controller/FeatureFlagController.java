package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.config.AppFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.togglz.core.annotation.Label;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Expõe e gerencia o estado das Feature Flags para o Angular.
 * GET  /api/v1/features         — qualquer usuário autenticado consulta quais módulos estão ativos.
 * POST /api/v1/features/{name}/toggle — apenas ROLE_ADMIN pode ativar/desativar módulos.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureManager featureManager;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listFeatures() {
        Map<String, Object> features = new LinkedHashMap<>();
        for (AppFeatures feature : AppFeatures.values()) {
            features.put(feature.name(), Map.of(
                "active", feature.isActive(),
                "label", resolveLabel(feature)
            ));
        }
        return ResponseEntity.ok(features);
    }

    @PostMapping("/{name}/toggle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleFeature(@PathVariable String name) {
        AppFeatures feature;
        try {
            feature = AppFeatures.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        boolean newState = !featureManager.isActive(feature);
        featureManager.setFeatureState(new FeatureState(feature, newState));
        log.info("Feature flag '{}' alterada para: {}", feature.name(), newState);
        return ResponseEntity.ok(Map.of(
            "name", feature.name(),
            "active", newState,
            "label", resolveLabel(feature)
        ));
    }

    private String resolveLabel(AppFeatures feature) {
        try {
            Field field = AppFeatures.class.getField(feature.name());
            Label label = field.getAnnotation(Label.class);
            return label != null ? label.value() : feature.name();
        } catch (NoSuchFieldException e) {
            log.warn("Campo não encontrado para feature: {}", feature.name());
            return feature.name();
        }
    }
}
