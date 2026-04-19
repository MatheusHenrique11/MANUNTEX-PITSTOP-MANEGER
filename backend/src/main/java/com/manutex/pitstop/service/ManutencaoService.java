package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.entity.Veiculo;
import com.manutex.pitstop.domain.enums.StatusManutencao;
import com.manutex.pitstop.domain.repository.ManutencaoRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.domain.repository.VeiculoRepository;
import com.manutex.pitstop.web.dto.ManutencaoRequest;
import com.manutex.pitstop.web.dto.ManutencaoResponse;
import com.manutex.pitstop.web.dto.ManutencaoUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManutencaoService {

    private final ManutencaoRepository manutencaoRepository;
    private final VeiculoRepository veiculoRepository;
    private final UserRepository userRepository;

    @Transactional
    public ManutencaoResponse criar(ManutencaoRequest request) {
        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
            .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado: " + request.veiculoId()));
        User mecanico = userRepository.findById(request.mecanicoId())
            .orElseThrow(() -> new EntityNotFoundException("Mecânico não encontrado: " + request.mecanicoId()));

        Manutencao m = Manutencao.builder()
            .veiculo(veiculo)
            .mecanico(mecanico)
            .descricao(request.descricao())
            .kmEntrada(request.kmEntrada())
            .relatorio(request.relatorio())
            .observacoes(request.observacoes())
            .orcamento(request.orcamento())
            .valorFinal(request.valorFinal())
            .kmSaida(request.kmSaida())
            .build();

        Manutencao saved = manutencaoRepository.save(m);
        log.info("OS criada: id={}, veiculo={}, mecanico={}", saved.getId(), veiculo.getPlaca(), mecanico.getFullName());
        return ManutencaoResponse.of(saved);
    }

    @Transactional
    public ManutencaoResponse atualizar(UUID id, ManutencaoUpdateRequest request) {
        Manutencao m = findOrThrow(id);
        assertEditavel(m);

        if (request.mecanicoId() != null) {
            User mecanico = userRepository.findById(request.mecanicoId())
                .orElseThrow(() -> new EntityNotFoundException("Mecânico não encontrado: " + request.mecanicoId()));
            m.setMecanico(mecanico);
        }
        if (request.descricao() != null)  m.setDescricao(request.descricao());
        if (request.kmEntrada() != null)  m.setKmEntrada(request.kmEntrada());
        if (request.kmSaida() != null)    m.setKmSaida(request.kmSaida());
        if (request.relatorio() != null)  m.setRelatorio(request.relatorio());
        if (request.observacoes() != null) m.setObservacoes(request.observacoes());
        if (request.orcamento() != null)  m.setOrcamento(request.orcamento());
        if (request.valorFinal() != null) m.setValorFinal(request.valorFinal());

        return ManutencaoResponse.of(manutencaoRepository.save(m));
    }

    @Transactional
    public ManutencaoResponse alterarStatus(UUID id, StatusManutencao novoStatus) {
        Manutencao m = findOrThrow(id);

        if (isTerminal(m.getStatus())) {
            throw new StatusTransitionException(
                "OS %s está em estado terminal '%s' e não pode ser alterada.".formatted(id, m.getStatus()));
        }
        if (novoStatus == StatusManutencao.CONCLUIDA && (m.getRelatorio() == null || m.getRelatorio().isBlank())) {
            throw new StatusTransitionException("Informe o relatório do serviço antes de concluir a OS.");
        }

        m.setStatus(novoStatus);
        log.info("OS {} status alterado para {}", id, novoStatus);
        return ManutencaoResponse.of(manutencaoRepository.save(m));
    }

    @Transactional(readOnly = true)
    public ManutencaoResponse buscarPorId(UUID id) {
        return ManutencaoResponse.of(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ManutencaoResponse> listar(StatusManutencao status, Pageable pageable) {
        Page<Manutencao> page = status != null
            ? manutencaoRepository.findByStatus(status, pageable)
            : manutencaoRepository.findAll(pageable);
        return page.map(ManutencaoResponse::of);
    }

    @Transactional(readOnly = true)
    public Page<ManutencaoResponse> listarPorVeiculo(UUID veiculoId, StatusManutencao status, Pageable pageable) {
        Page<Manutencao> page = status != null
            ? manutencaoRepository.findByVeiculoIdAndStatus(veiculoId, status, pageable)
            : manutencaoRepository.findByVeiculoId(veiculoId, pageable);
        return page.map(ManutencaoResponse::of);
    }

    @Transactional
    public void deletar(UUID id) {
        Manutencao m = findOrThrow(id);
        manutencaoRepository.delete(m);
        log.info("OS deletada: id={}", id);
    }

    private Manutencao findOrThrow(UUID id) {
        return manutencaoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("OS não encontrada: " + id));
    }

    private void assertEditavel(Manutencao m) {
        if (isTerminal(m.getStatus())) {
            throw new StatusTransitionException(
                "OS %s está em estado terminal e não pode ser editada.".formatted(m.getId()));
        }
    }

    private boolean isTerminal(StatusManutencao status) {
        return status == StatusManutencao.CONCLUIDA || status == StatusManutencao.CANCELADA;
    }

    public static class StatusTransitionException extends RuntimeException {
        public StatusTransitionException(String message) { super(message); }
    }
}
