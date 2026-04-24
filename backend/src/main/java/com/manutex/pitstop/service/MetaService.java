package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.Manutencao;
import com.manutex.pitstop.domain.entity.MetaMecanico;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.EmpresaRepository;
import com.manutex.pitstop.domain.repository.ManutencaoRepository;
import com.manutex.pitstop.domain.repository.MetaMecanicoRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.security.TenantContext;
import com.manutex.pitstop.web.dto.MetaRequest;
import com.manutex.pitstop.web.dto.MetaResponse;
import com.manutex.pitstop.web.dto.ProducaoMecanicoResponse;
import com.manutex.pitstop.web.dto.ServicoMecanicoItem;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaService {

    private final MetaMecanicoRepository metaRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final PdfRelatorioService pdfRelatorioService;
    private final EmpresaConfigService empresaConfigService;

    @Transactional
    public MetaResponse definirMeta(MetaRequest request) {
        UUID empresaId = TenantContext.requireEmpresaId();

        User mecanico = userRepository.findById(request.mecanicoId())
            .orElseThrow(() -> new EntityNotFoundException("Mecânico não encontrado: " + request.mecanicoId()));

        if (mecanico.getRole() != UserRole.ROLE_MECANICO) {
            throw new IllegalArgumentException("Usuário informado não é um mecânico.");
        }

        UUID mecEmpresaId = mecanico.getEmpresa() != null ? mecanico.getEmpresa().getId() : null;
        if (!empresaId.equals(mecEmpresaId)) {
            throw new AccessDeniedException("Mecânico não pertence à empresa do contexto.");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada."));

        MetaMecanico meta = metaRepository
            .findByMecanicoIdAndMesAndAno(request.mecanicoId(), request.mes(), request.ano())
            .orElseGet(() -> MetaMecanico.builder()
                .mecanico(mecanico)
                .empresa(empresa)
                .mes(request.mes())
                .ano(request.ano())
                .build());

        meta.setValorMeta(request.valorMeta());
        MetaMecanico saved = metaRepository.save(meta);

        log.info("Meta definida: mecanico={}, mes={}/{}, valor={}",
            mecanico.getFullName(), request.mes(), request.ano(), request.valorMeta());

        return MetaResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public ProducaoMecanicoResponse buscarProducaoMecanico(UUID mecanicoId, int mes, int ano) {
        UUID empresaId = TenantContext.requireEmpresaId();
        String roleAtual = roleAtual();

        if (UserRole.ROLE_MECANICO.name().equals(roleAtual)) {
            UUID idAutenticado = idUsuarioAutenticado();
            if (!idAutenticado.equals(mecanicoId)) {
                throw new AccessDeniedException("Mecânico só pode consultar sua própria produção.");
            }
        }

        User mecanico = userRepository.findById(mecanicoId)
            .orElseThrow(() -> new EntityNotFoundException("Mecânico não encontrado: " + mecanicoId));

        return montarProducao(mecanico, mes, ano);
    }

    @Transactional(readOnly = true)
    public ProducaoMecanicoResponse buscarMinhaProducao(int mes, int ano) {
        UUID mecanicoId = idUsuarioAutenticado();
        TenantContext.requireEmpresaId();

        User mecanico = userRepository.findById(mecanicoId)
            .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));

        return montarProducao(mecanico, mes, ano);
    }

    @Transactional(readOnly = true)
    public List<ProducaoMecanicoResponse> listarProducaoTodosMecanicos(int mes, int ano) {
        UUID empresaId = TenantContext.requireEmpresaId();

        List<User> mecanicos = userRepository.findByEmpresaId(empresaId).stream()
            .filter(u -> u.getRole() == UserRole.ROLE_MECANICO && u.isEnabled())
            .toList();

        // Inclui mecânicos com meta definida e/ou com produção no período
        List<UUID> comProducao = manutencaoRepository.findMecanicoIdsComProducao(empresaId, mes, ano);
        Set<UUID> comMeta = metaRepository.findByEmpresaIdAndMesAndAno(empresaId, mes, ano)
            .stream().map(m -> m.getMecanico().getId()).collect(Collectors.toSet());

        List<User> relevantes = mecanicos.stream()
            .filter(u -> comMeta.contains(u.getId()) || comProducao.contains(u.getId()))
            .toList();

        return relevantes.stream()
            .map(m -> montarProducao(m, mes, ano))
            .toList();
    }

    public byte[] gerarPdfRelatorio(int mes, int ano) {
        UUID empresaId = TenantContext.requireEmpresaId();
        List<ProducaoMecanicoResponse> producoes = listarProducaoTodosMecanicos(mes, ano);

        String nomeEmpresa;
        try {
            nomeEmpresa = empresaConfigService.buscar().nome();
        } catch (Exception e) {
            nomeEmpresa = "Empresa";
        }

        if (producoes.isEmpty()) {
            log.warn("PDF solicitado sem dados: empresaId={}, mes={}, ano={}", empresaId, mes, ano);
        }

        return pdfRelatorioService.gerarRelatorioMensal(mes, ano, nomeEmpresa, producoes);
    }

    @Transactional(readOnly = true)
    public MetaResponse buscarMeta(UUID mecanicoId, int mes, int ano) {
        TenantContext.requireEmpresaId();
        String roleAtual = roleAtual();

        if (UserRole.ROLE_MECANICO.name().equals(roleAtual)) {
            UUID idAutenticado = idUsuarioAutenticado();
            if (!idAutenticado.equals(mecanicoId)) {
                throw new AccessDeniedException("Mecânico só pode consultar sua própria meta.");
            }
        }

        return metaRepository.findByMecanicoIdAndMesAndAno(mecanicoId, mes, ano)
            .map(MetaResponse::of)
            .orElseThrow(() -> new EntityNotFoundException(
                "Meta não encontrada para o mecânico no período informado."));
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private ProducaoMecanicoResponse montarProducao(User mecanico, int mes, int ano) {
        List<Manutencao> servicos = manutencaoRepository
            .findConcluidasByMecanicoAndPeriodo(mecanico.getId(), mes, ano);

        BigDecimal totalProduzido = servicos.stream()
            .map(s -> s.getValorFinal() != null ? s.getValorFinal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorMeta = metaRepository
            .findByMecanicoIdAndMesAndAno(mecanico.getId(), mes, ano)
            .map(MetaMecanico::getValorMeta)
            .orElse(null);

        double percentual = 0.0;
        boolean metaBatida = false;
        if (valorMeta != null && valorMeta.compareTo(BigDecimal.ZERO) > 0) {
            percentual = totalProduzido
                .multiply(BigDecimal.valueOf(100))
                .divide(valorMeta, 2, RoundingMode.HALF_UP)
                .doubleValue();
            metaBatida = totalProduzido.compareTo(valorMeta) >= 0;
        }

        List<ServicoMecanicoItem> items = servicos.stream()
            .map(ServicoMecanicoItem::of)
            .toList();

        return new ProducaoMecanicoResponse(
            mecanico.getId(),
            mecanico.getFullName(),
            mes,
            ano,
            servicos.size(),
            totalProduzido,
            valorMeta,
            percentual,
            metaBatida,
            items
        );
    }

    private String roleAtual() {
        return SecurityContextHolder.getContext().getAuthentication()
            .getAuthorities().stream()
            .map(a -> a.getAuthority())
            .findFirst()
            .orElse("");
    }

    private UUID idUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .map(User::getId)
            .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
    }
}
