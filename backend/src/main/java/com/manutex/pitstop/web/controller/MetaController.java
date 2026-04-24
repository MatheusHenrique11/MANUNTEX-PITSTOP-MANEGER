package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.service.MetaService;
import com.manutex.pitstop.web.dto.MetaRequest;
import com.manutex.pitstop.web.dto.MetaResponse;
import com.manutex.pitstop.web.dto.ProducaoMecanicoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metas")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    /**
     * GERENTE/ADMIN: define ou atualiza a meta mensal de um mecânico.
     * Upsert — cria se não existir, atualiza se já existir para aquele mês/ano.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
    public ResponseEntity<MetaResponse> definirMeta(@Valid @RequestBody MetaRequest request) {
        return ResponseEntity.ok(metaService.definirMeta(request));
    }

    /**
     * GERENTE/ADMIN: lista a produção de TODOS os mecânicos no período.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
    public ResponseEntity<List<ProducaoMecanicoResponse>> listarProducaoGeral(
        @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().monthValue}") int mes,
        @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().year}") int ano
    ) {
        return ResponseEntity.ok(metaService.listarProducaoTodosMecanicos(mes, ano));
    }

    /**
     * GERENTE/ADMIN: detalha a produção de um mecânico específico.
     * MECANICO: pode consultar SOMENTE a própria produção — o service valida isso.
     */
    @GetMapping("/mecanico/{mecanicoId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_MECANICO')")
    public ResponseEntity<ProducaoMecanicoResponse> buscarProducaoMecanico(
        @PathVariable UUID mecanicoId,
        @RequestParam(required = false) Integer mes,
        @RequestParam(required = false) Integer ano
    ) {
        int mesAtual = mes != null ? mes : LocalDate.now().getMonthValue();
        int anoAtual = ano != null ? ano : LocalDate.now().getYear();
        return ResponseEntity.ok(metaService.buscarProducaoMecanico(mecanicoId, mesAtual, anoAtual));
    }

    /**
     * MECANICO: consulta a própria produção sem precisar informar o ID.
     */
    @GetMapping("/minhas")
    @PreAuthorize("hasAnyRole('ROLE_MECANICO', 'ROLE_ADMIN', 'ROLE_GERENTE')")
    public ResponseEntity<ProducaoMecanicoResponse> buscarMinhaProducao(
        @RequestParam(required = false) Integer mes,
        @RequestParam(required = false) Integer ano
    ) {
        int mesAtual = mes != null ? mes : LocalDate.now().getMonthValue();
        int anoAtual = ano != null ? ano : LocalDate.now().getYear();
        return ResponseEntity.ok(metaService.buscarMinhaProducao(mesAtual, anoAtual));
    }

    /**
     * GERENTE/ADMIN: consulta a meta definida para um mecânico em determinado período.
     */
    @GetMapping("/mecanico/{mecanicoId}/meta")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_MECANICO')")
    public ResponseEntity<MetaResponse> buscarMeta(
        @PathVariable UUID mecanicoId,
        @RequestParam int mes,
        @RequestParam int ano
    ) {
        return ResponseEntity.ok(metaService.buscarMeta(mecanicoId, mes, ano));
    }

    /**
     * GERENTE/ADMIN: gera PDF do relatório mensal de metas para envio ao RH.
     */
    @GetMapping("/relatorio/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_GERENTE')")
    public ResponseEntity<byte[]> gerarPdfRelatorio(
        @RequestParam(required = false) Integer mes,
        @RequestParam(required = false) Integer ano
    ) {
        int mesAlvo = mes != null ? mes : LocalDate.now().getMonthValue();
        int anoAlvo = ano != null ? ano : LocalDate.now().getYear();

        byte[] pdf = metaService.gerarPdfRelatorio(mesAlvo, anoAlvo);

        String filename = String.format("relatorio-metas-%02d-%d.pdf", mesAlvo, anoAlvo);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/pdf"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(pdf);
    }
}
