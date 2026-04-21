package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.domain.repository.ManutencaoRepository;
import com.manutex.pitstop.web.dto.RastreioResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/rastreio")
@RequiredArgsConstructor
public class RastreioController {

    private final ManutencaoRepository manutencaoRepository;

    @GetMapping("/{token}")
    public ResponseEntity<RastreioResponse> rastrear(@PathVariable UUID token) {
        var manutencao = manutencaoRepository.findByTrackingToken(token)
            .orElseThrow(() -> new EntityNotFoundException(
                "Nenhuma ordem de serviço encontrada para este link de rastreio."));
        return ResponseEntity.ok(RastreioResponse.of(manutencao));
    }
}
