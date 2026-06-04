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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.togglz.core.context.FeatureContext;
import org.togglz.core.manager.FeatureManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationTemplateRepository      templateRepository;
    @Mock NotificationLogRepository           logRepository;
    @Mock EmpresaNotificationConfigRepository notifConfigRepository;
    @Mock AesEncryptionService                aesEncryption;
    @Mock NotificationProvider                mockProvider;

    // Construção manual — @InjectMocks não injeta List<T> corretamente
    NotificationService service;

    private final UUID empresaId = UUID.randomUUID();
    private final UUID osId      = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // provider retorna WHATSAPP — lenient para não falhar em testes que não chegam ao send()
        lenient().when(mockProvider.getChannel()).thenReturn(NotificationChannel.WHATSAPP);
        service = new NotificationService(
            templateRepository, logRepository, notifConfigRepository,
            aesEncryption, List.of(mockProvider)
        );
    }

    private OsNotificationEvent buildEvent(NotificationEvent tipo) {
        return new OsNotificationEvent(
            empresaId, osId, clienteId,
            "11999990000", "cliente@email.com",
            "João Silva", "ABC-1234", "Gol",
            "https://managerpitstop.com.br/rastreio/abc", tipo
        );
    }

    // ── 1. Feature Flag desativada → nenhum acesso ao repositório ────────────

    @Test
    void deveIgnorarDispatchQuandoFeatureFlagDesativada() {
        try (var ctx = mockStatic(FeatureContext.class)) {
            FeatureManager fm = mock(FeatureManager.class);
            ctx.when(FeatureContext::getFeatureManager).thenReturn(fm);
            when(fm.isActive(AppFeatures.NOTIFICATIONS)).thenReturn(false);

            service.dispatch(buildEvent(NotificationEvent.OS_CRIADA));

            verifyNoInteractions(templateRepository, logRepository, notifConfigRepository);
        }
    }

    // ── 2. Template ativo → envia + persiste log com destinatário mascarado ──

    @Test
    void deveEnviarELogarComDestinatarioMascarado() {
        try (var ctx = mockStatic(FeatureContext.class)) {
            notificationsEnabled(ctx);

            NotificationTemplate tmpl = templateWhatsApp(NotificationEvent.OS_CRIADA,
                "OS Aberta", "Olá {{cliente_nome}}! OS para {{veiculo_placa}}.");

            when(notifConfigRepository.findByEmpresaId(empresaId))
                .thenReturn(Optional.of(configWhatsApp("cipher")));
            when(aesEncryption.decryptFromBase64("cipher")).thenReturn("plain_token");
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
                empresaId, NotificationEvent.OS_CRIADA, NotificationChannel.WHATSAPP))
                .thenReturn(Optional.of(tmpl));
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
                empresaId, NotificationEvent.OS_CRIADA, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
            when(mockProvider.send(any(), any(), any(), any()))
                .thenReturn(NotificationProvider.NotificationResult.ok());
            when(logRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.dispatch(buildEvent(NotificationEvent.OS_CRIADA));

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(logRepository).save(captor.capture());
            NotificationLog log = captor.getValue();
            assertThat(log.getStatus()).isEqualTo(NotificationStatus.ENVIADO);
            // Telefone deve estar mascarado (não contém número completo)
            assertThat(log.getDestinatario()).doesNotContain("11999990000");
            assertThat(log.getDestinatario()).isNotBlank();
        }
    }

    // ── 3. Destinatário null → log REJEITADO sem tentar enviar ───────────────

    @Test
    void deveRegistrarRejeitadoQuandoSemTelefone() {
        try (var ctx = mockStatic(FeatureContext.class)) {
            notificationsEnabled(ctx);

            NotificationTemplate tmpl = templateWhatsApp(NotificationEvent.OS_CRIADA,
                null, "Corpo sem telefone.");

            when(notifConfigRepository.findByEmpresaId(empresaId)).thenReturn(Optional.empty());
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
                empresaId, NotificationEvent.OS_CRIADA, NotificationChannel.WHATSAPP))
                .thenReturn(Optional.of(tmpl));
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
                empresaId, NotificationEvent.OS_CRIADA, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
            when(logRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            OsNotificationEvent semTelefone = new OsNotificationEvent(
                empresaId, osId, clienteId, null, null,
                "João", "ABC-1234", "Gol", null, NotificationEvent.OS_CRIADA);

            service.dispatch(semTelefone);

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(logRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.REJEITADO);
            verify(mockProvider, never()).send(any(), any(), any(), any());
        }
    }

    // ── 4. Provider falha → log FALHOU, transação da OS não é afetada ────────

    @Test
    void devePersistirFalhaQuandoProviderRetornaErro() {
        try (var ctx = mockStatic(FeatureContext.class)) {
            notificationsEnabled(ctx);

            NotificationTemplate tmpl = templateWhatsApp(NotificationEvent.OS_CONCLUIDA,
                "Pronto!", "Veículo pronto.");

            when(notifConfigRepository.findByEmpresaId(empresaId))
                .thenReturn(Optional.of(configWhatsApp("cipher")));
            when(aesEncryption.decryptFromBase64("cipher")).thenReturn("tok");
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
                empresaId, NotificationEvent.OS_CONCLUIDA, NotificationChannel.WHATSAPP))
                .thenReturn(Optional.of(tmpl));
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
                empresaId, NotificationEvent.OS_CONCLUIDA, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
            when(mockProvider.send(any(), any(), any(), any()))
                .thenReturn(NotificationProvider.NotificationResult.fail("Connection timeout"));
            when(logRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.dispatch(buildEvent(NotificationEvent.OS_CONCLUIDA));

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(logRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FALHOU);
            assertThat(captor.getValue().getErrorMessage()).contains("Connection timeout");
        }
    }

    // ── 5. Template inativo → não despacha ───────────────────────────────────

    @Test
    void deveNaoEnviarQuandoNenhumTemplatAtivoEncontrado() {
        try (var ctx = mockStatic(FeatureContext.class)) {
            notificationsEnabled(ctx);

            when(notifConfigRepository.findByEmpresaId(empresaId)).thenReturn(Optional.empty());
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(any(), any(), any()))
                .thenReturn(Optional.empty());

            service.dispatch(buildEvent(NotificationEvent.OS_CRIADA));

            verifyNoInteractions(logRepository);
            verify(mockProvider, never()).send(any(), any(), any(), any());
        }
    }

    // ── 6. seed idempotente — não duplica se já existe ───────────────────────

    @Test
    void seedNaoCriaDuplicadosSeTemplateJaExiste() {
        when(templateRepository.existsByEmpresaIdAndEventoAndCanal(any(), any(), any()))
            .thenReturn(true);

        service.seedDefaultTemplates(empresaId);

        verify(templateRepository, never()).save(any());
    }

    @Test
    void seedCriaTodasAsCombinacoes() {
        when(templateRepository.existsByEmpresaIdAndEventoAndCanal(any(), any(), any()))
            .thenReturn(false);
        when(templateRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.seedDefaultTemplates(empresaId);

        int esperado = NotificationEvent.values().length * 2; // WHATSAPP + EMAIL por evento
        verify(templateRepository, times(esperado)).save(any());
    }

    // ── 7. updateTemplate rejeita template de outra empresa ──────────────────

    @Test
    void updateTemplateRejeita_TemplateDeOutraEmpresa() {
        UUID outraEmpresa = UUID.randomUUID();
        NotificationTemplate tmpl = NotificationTemplate.builder()
            .id(UUID.randomUUID()).empresaId(outraEmpresa).corpo("x").ativo(true).build();

        when(templateRepository.findById(tmpl.getId())).thenReturn(Optional.of(tmpl));

        assertThatThrownBy(() ->
            service.updateTemplate(tmpl.getId(), empresaId, "titulo", "corpo", true)
        ).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    // ── 8. Interpolação de variáveis não quebra com placeholder inválido ──────

    @Test
    void deveEnviarMesmoComPlaceholderInvalido() {
        try (var ctx = mockStatic(FeatureContext.class)) {
            notificationsEnabled(ctx);

            // Template com placeholder que não existe — deve enviar sem quebrar
            NotificationTemplate tmpl = templateWhatsApp(NotificationEvent.OS_CRIADA,
                null, "Olá {{nome_invalido}}! Placa: {{veiculo_placa}}.");

            when(notifConfigRepository.findByEmpresaId(empresaId))
                .thenReturn(Optional.of(configWhatsApp("cipher")));
            when(aesEncryption.decryptFromBase64("cipher")).thenReturn("tok");
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
                empresaId, NotificationEvent.OS_CRIADA, NotificationChannel.WHATSAPP))
                .thenReturn(Optional.of(tmpl));
            when(templateRepository.findByEmpresaIdAndEventoAndCanalAndAtivoTrue(
                empresaId, NotificationEvent.OS_CRIADA, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());

            ArgumentCaptor<String> corpoCaptor = ArgumentCaptor.forClass(String.class);
            when(mockProvider.send(any(), any(), corpoCaptor.capture(), any()))
                .thenReturn(NotificationProvider.NotificationResult.ok());
            when(logRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.dispatch(buildEvent(NotificationEvent.OS_CRIADA));

            // Placeholder inválido permanece; placeholder válido é substituído
            assertThat(corpoCaptor.getValue())
                .contains("{{nome_invalido}}")  // não substituído
                .contains("ABC-1234");           // substituído corretamente
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void notificationsEnabled(org.mockito.MockedStatic<FeatureContext> ctx) {
        FeatureManager fm = mock(FeatureManager.class);
        ctx.when(FeatureContext::getFeatureManager).thenReturn(fm);
        when(fm.isActive(AppFeatures.NOTIFICATIONS)).thenReturn(true);
    }

    private NotificationTemplate templateWhatsApp(NotificationEvent evento,
                                                   String titulo, String corpo) {
        return NotificationTemplate.builder()
            .id(UUID.randomUUID()).empresaId(empresaId)
            .evento(evento).canal(NotificationChannel.WHATSAPP)
            .titulo(titulo).corpo(corpo).ativo(true).build();
    }

    private EmpresaNotificationConfig configWhatsApp(String tokenCipher) {
        return EmpresaNotificationConfig.builder()
            .empresaId(empresaId)
            .whatsappProviderUrl("https://api.evolution.com")
            .whatsappApiToken(tokenCipher)
            .whatsappInstanceName("pitstop")
            .build();
    }
}
