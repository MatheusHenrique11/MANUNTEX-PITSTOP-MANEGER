package com.manutex.pitstop.service;

import com.manutex.pitstop.config.AppFeatures;
import com.manutex.pitstop.domain.entity.EmpresaNotificationConfig;
import com.manutex.pitstop.domain.entity.NotificationLog;
import com.manutex.pitstop.domain.entity.NotificationTemplate;
import com.manutex.pitstop.domain.enums.NotificationChannel;
import com.manutex.pitstop.domain.enums.NotificationEvent;
import com.manutex.pitstop.domain.enums.NotificationStatus;
import com.manutex.pitstop.domain.repository.EmpresaNotificationConfigRepository;
import com.manutex.pitstop.domain.repository.NotificationLogRepository;
import com.manutex.pitstop.domain.repository.NotificationTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestra o pipeline de notificações:
 *  1. Verifica se a feature flag NOTIFICATIONS está ativa
 *  2. Resolve o template configurado (empresa + evento + canal)
 *  3. Interpola as variáveis no corpo
 *  4. Despacha para o NotificationProvider correto
 *  5. Persiste NotificationLog com destinatário mascarado (LGPD)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTemplateRepository          templateRepository;
    private final NotificationLogRepository               logRepository;
    private final EmpresaNotificationConfigRepository     notifConfigRepository;
    private final AesEncryptionService                    aesEncryption;
    private final List<NotificationProvider>              providers;

    // ── Disparo ───────────────────────────────────────────────────────────────

    @Transactional
    public void dispatch(OsNotificationEvent event) {
        if (!AppFeatures.NOTIFICATIONS.isActive()) return;

        EmpresaNotificationConfig config =
            notifConfigRepository.findByEmpresaId(event.empresaId()).orElse(null);

        Map<String, String> providerConfig = buildProviderConfig(config);
        Map<String, String> variaveis      = buildVariaveis(event);

        for (NotificationChannel canal : List.of(NotificationChannel.WHATSAPP, NotificationChannel.EMAIL)) {
            templateRepository
                .findByEmpresaIdAndEventoAndCanalAndAtivoTrue(event.empresaId(), event.evento(), canal)
                .ifPresent(t -> sendViaChannel(event, t, canal, variaveis, providerConfig));
        }
    }

    // ── Gestão de templates ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationTemplate> listTemplates(UUID empresaId) {
        return templateRepository.findByEmpresaIdOrderByEventoAscCanalAsc(empresaId);
    }

    @Transactional
    public NotificationTemplate updateTemplate(UUID templateId, UUID empresaId,
                                               String titulo, String corpo, boolean ativo) {
        NotificationTemplate t = templateRepository.findById(templateId)
            .filter(tmpl -> empresaId.equals(tmpl.getEmpresaId()))
            .orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + templateId));
        t.setTitulo(titulo);
        t.setCorpo(corpo);
        t.setAtivo(ativo);
        return templateRepository.save(t);
    }

    @Transactional
    public void seedDefaultTemplates(UUID empresaId) {
        for (NotificationEvent evento : NotificationEvent.values()) {
            for (NotificationChannel canal : List.of(NotificationChannel.WHATSAPP, NotificationChannel.EMAIL)) {
                // Usa existsByEmpresaIdAndEventoAndCanal (sem filtro ativo)
                // para não violar a UNIQUE constraint (empresa_id, evento, canal)
                boolean exists = templateRepository
                    .existsByEmpresaIdAndEventoAndCanal(empresaId, evento, canal);
                if (!exists) {
                    templateRepository.save(NotificationTemplate.builder()
                        .empresaId(empresaId)
                        .evento(evento)
                        .canal(canal)
                        .titulo(defaultTitulo(evento))
                        .corpo(defaultCorpo(evento, canal))
                        .ativo(false)
                        .build());
                }
            }
        }
    }

    // ── Configuração do provider ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EmpresaNotificationConfig getConfig(UUID empresaId) {
        return notifConfigRepository.findByEmpresaId(empresaId)
            .orElseGet(() -> EmpresaNotificationConfig.builder().empresaId(empresaId).build());
    }

    @Transactional
    public EmpresaNotificationConfig saveConfig(UUID empresaId,
                                                String whatsappUrl, String whatsappToken,
                                                String whatsappInstance, String emailFrom) {
        EmpresaNotificationConfig config = notifConfigRepository
            .findByEmpresaId(empresaId)
            .orElseGet(() -> EmpresaNotificationConfig.builder().empresaId(empresaId).build());
        config.setWhatsappProviderUrl(whatsappUrl);
        // Token cifrado em repouso; não alterar se request enviou vazio (não regrava)
        if (StringUtils.hasText(whatsappToken)) {
            config.setWhatsappApiToken(aesEncryption.encryptToBase64(whatsappToken));
        }
        config.setWhatsappInstanceName(whatsappInstance);
        config.setNotificationEmailFrom(emailFrom);
        return notifConfigRepository.save(config);
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationLog> listLogs(UUID empresaId,
                                           NotificationStatus status,
                                           NotificationChannel canal,
                                           NotificationEvent evento,
                                           Pageable pageable) {
        if (status == null && canal == null && evento == null) {
            return logRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId, pageable);
        }
        return logRepository.findFiltered(empresaId, status, canal, evento, pageable);
    }

    // ── Teste de envio ────────────────────────────────────────────────────────

    @Transactional
    public void sendTest(UUID templateId, UUID empresaId, String destinatario) {
        NotificationTemplate template = templateRepository.findById(templateId)
            .filter(t -> empresaId.equals(t.getEmpresaId()))
            .orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + templateId));

        EmpresaNotificationConfig config = notifConfigRepository.findByEmpresaId(empresaId).orElse(null);
        Map<String, String> providerConfig = buildProviderConfig(config);

        Map<String, String> variaveis = Map.of(
            "cliente_nome",   "Cliente Teste",
            "veiculo_placa",  "ABC-1234",
            "veiculo_modelo", "Modelo Teste",
            "os_link",        "https://managerpitstop.com.br/rastreio/teste",
            "status",         "TESTE"
        );

        sendViaChannel(
            new OsNotificationEvent(empresaId, null, null, destinatario, destinatario,
                "Cliente Teste", "ABC-1234", "Modelo Teste",
                "https://managerpitstop.com.br/rastreio/teste", template.getEvento()),
            template, template.getCanal(), variaveis, providerConfig
        );
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private void sendViaChannel(OsNotificationEvent event, NotificationTemplate template,
                                NotificationChannel canal, Map<String, String> variaveis,
                                Map<String, String> config) {
        String destinatario = resolveDestinatario(event, canal);
        if (destinatario == null || destinatario.isBlank()) {
            persistLog(event, canal, null, NotificationStatus.REJEITADO,
                "Destinatário não disponível para canal " + canal);
            return;
        }

        String corpo  = interpolate(template.getCorpo(), variaveis);
        String titulo = template.getTitulo() != null ? interpolate(template.getTitulo(), variaveis) : null;

        NotificationProvider provider = providers.stream()
            .filter(p -> p.getChannel() == canal)
            .findFirst()
            .orElse(null);

        if (provider == null) {
            persistLog(event, canal, destinatario, NotificationStatus.REJEITADO,
                "Nenhum provider disponível para canal " + canal);
            return;
        }

        NotificationProvider.NotificationResult result =
            provider.send(destinatario, titulo, corpo, config);

        persistLog(event, canal, destinatario,
            result.success() ? NotificationStatus.ENVIADO : NotificationStatus.FALHOU,
            result.errorMessage());
    }

    private String resolveDestinatario(OsNotificationEvent event, NotificationChannel canal) {
        return switch (canal) {
            case WHATSAPP, SMS -> event.clienteTelefone();
            case EMAIL         -> event.clienteEmail();
            default            -> null;
        };
    }

    private void persistLog(OsNotificationEvent event, NotificationChannel canal,
                            String destinatario, NotificationStatus status, String error) {
        logRepository.save(NotificationLog.builder()
            .empresaId(event.empresaId())
            .manutencaoId(event.manutencaoId())
            .clienteId(event.clienteId())
            .evento(event.evento())
            .canal(canal)
            .destinatario(mask(destinatario, canal))
            .status(status)
            .errorMessage(error)
            .enviadoEm(status == NotificationStatus.ENVIADO ? Instant.now() : null)
            .build());
    }

    private String interpolate(String texto, Map<String, String> vars) {
        String result = texto;
        for (var entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private String mask(String valor, NotificationChannel canal) {
        if (valor == null || valor.length() <= 4) return "****";
        String suffix = valor.substring(valor.length() - 4);
        return canal == NotificationChannel.EMAIL
            ? "****@" + valor.replaceAll(".*@", "")
            : "****" + suffix;
    }

    private Map<String, String> buildVariaveis(OsNotificationEvent event) {
        return Map.of(
            "cliente_nome",   nvl(event.clienteNome()),
            "veiculo_placa",  nvl(event.veiculoPlaca()),
            "veiculo_modelo", nvl(event.veiculoModelo()),
            "os_link",        nvl(event.rastreioLink()),
            "status",         event.evento().name().replace("OS_", "")
        );
    }

    private Map<String, String> buildProviderConfig(EmpresaNotificationConfig config) {
        if (config == null) return Map.of();
        String decryptedToken = config.getWhatsappApiToken() != null
            ? nvl(aesEncryption.decryptFromBase64(config.getWhatsappApiToken()))
            : "";
        return Map.of(
            "whatsapp_provider_url",   nvl(config.getWhatsappProviderUrl()),
            "whatsapp_api_token",      decryptedToken,
            "whatsapp_instance_name",  nvl(config.getWhatsappInstanceName()),
            "notification_email_from", nvl(config.getNotificationEmailFrom())
        );
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    // ── Templates padrão ─────────────────────────────────────────────────────

    private String defaultTitulo(NotificationEvent evento) {
        return switch (evento) {
            case OS_CRIADA            -> "OS aberta";
            case OS_EM_ANDAMENTO      -> "Serviço em andamento";
            case OS_AGUARDANDO_PECAS  -> "Aguardando peças";
            case OS_CONCLUIDA         -> "Veículo pronto!";
            case OS_CANCELADA         -> "OS cancelada";
            case DOCUMENTO_VENCENDO   -> "Documento próximo do vencimento";
            case ORCAMENTO_DISPONIVEL -> "Orçamento disponível";
        };
    }

    private String defaultCorpo(NotificationEvent evento, NotificationChannel canal) {
        boolean wpp = canal == NotificationChannel.WHATSAPP;
        return switch (evento) {
            case OS_CRIADA -> wpp
                ? "Olá {{cliente_nome}}! Sua OS foi aberta para {{veiculo_placa}}. Acompanhe: {{os_link}}"
                : "Olá {{cliente_nome}},\n\nSua OS foi aberta para o veículo {{veiculo_placa}}.\n\nAcompanhe: {{os_link}}";
            case OS_EM_ANDAMENTO -> wpp
                ? "{{cliente_nome}}, seu {{veiculo_placa}} está em serviço! {{os_link}}"
                : "Olá {{cliente_nome}},\n\nO serviço no veículo {{veiculo_placa}} foi iniciado.\n\nAcompanhe: {{os_link}}";
            case OS_AGUARDANDO_PECAS -> wpp
                ? "{{cliente_nome}}, seu {{veiculo_placa}} aguarda peças. Em breve continuamos!"
                : "Olá {{cliente_nome}},\n\nAguardamos peças para o veículo {{veiculo_placa}}. Avisaremos assim que chegarem.";
            case OS_CONCLUIDA -> wpp
                ? "{{cliente_nome}}, seu {{veiculo_placa}} está PRONTO! Pode retirar. {{os_link}}"
                : "Olá {{cliente_nome}},\n\nSeu veículo {{veiculo_placa}} está pronto para retirada!\n\nDetalhes: {{os_link}}";
            case OS_CANCELADA -> wpp
                ? "{{cliente_nome}}, a OS do {{veiculo_placa}} foi cancelada. Entre em contato conosco."
                : "Olá {{cliente_nome}},\n\nA OS para o veículo {{veiculo_placa}} foi cancelada.\n\nEntre em contato para mais informações.";
            case DOCUMENTO_VENCENDO -> wpp
                ? "{{cliente_nome}}, documento do {{veiculo_placa}} vence em breve. Regularize!"
                : "Olá {{cliente_nome}},\n\nO documento do veículo {{veiculo_placa}} está próximo do vencimento.";
            case ORCAMENTO_DISPONIVEL -> wpp
                ? "{{cliente_nome}}, orçamento do {{veiculo_placa}} disponível! Acesse: {{os_link}}"
                : "Olá {{cliente_nome}},\n\nSeu orçamento para {{veiculo_placa}} está disponível.\n\nAcesse: {{os_link}}";
        };
    }
}
