package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.config.AppFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.togglz.core.annotation.Label;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Expõe o estado das Feature Flags para o Angular.
 * GET /api/v1/features — qualquer usuário autenticado consulta quais módulos estão ativos.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureFlagController {

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

    // O toggle real é feito via painel Togglz em /admin/toggles
    // Este endpoint apenas lê o estado para o Angular renderizar o menu
}
