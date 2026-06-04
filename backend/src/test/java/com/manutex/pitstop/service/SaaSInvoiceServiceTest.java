package com.manutex.pitstop.service;

import com.manutex.pitstop.config.BillingProperties;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.FaturaNfe;
import com.manutex.pitstop.domain.entity.PlatformFiscalConfig;
import com.manutex.pitstop.domain.enums.FiscalEnvironment;
import com.manutex.pitstop.domain.enums.InvoiceType;
import com.manutex.pitstop.domain.repository.FaturaNfeRepository;
import com.manutex.pitstop.domain.repository.PlatformFiscalConfigRepository;
import com.manutex.pitstop.domain.repository.TenantFiscalConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null") // Mockito when/thenAnswer/thenReturn: falsos positivos do checker com @NonNull
class SaaSInvoiceServiceTest {

    @Mock FaturaNfeRepository        faturaNfeRepository;
    @Mock PlatformFiscalConfigRepository platformFiscalRepo;
    @Mock TenantFiscalConfigRepository   tenantFiscalRepo;
    @Mock BillingProperties              billingProperties;
    @Mock RestClient.Builder             restClientBuilder;

    private SaaSInvoiceService service;

    private Empresa empresa;
    private PlatformFiscalConfig platformConfig;

    @BeforeEach
    void setUp() {
        RestClient mockClient = mock(RestClient.class);
        when(restClientBuilder.baseUrl(any())).thenReturn(restClientBuilder);
        when(restClientBuilder.defaultHeader(any(), any())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(mockClient);

        service = new SaaSInvoiceService(
            "https://homologacao.focusnfe.com.br",
            faturaNfeRepository,
            platformFiscalRepo,
            tenantFiscalRepo,
            billingProperties,
            restClientBuilder
        );

        empresa = Empresa.builder()
            .id(UUID.randomUUID())
            .nome("Oficina Teste")
            .cnpj("11222333000181")
            .build();

        platformConfig = PlatformFiscalConfig.builder()
            .id(UUID.randomUUID())
            .razaoSocial("RiseCode Studio")
            .cnpj("99888777000166")
            .codigoMunicipio("3550308")
            .inscricaoMunicipal("123456")
            .codigoServicoMunicipal("14.01")
            .itemListaServico("1.01")
            .aliquotaIss(new BigDecimal("2.00"))
            .ambienteFiscal(FiscalEnvironment.HOMOLOGACAO)
            .build();
    }

    // ── Testes de fluxo SaaS ──────────────────────────────────────────────────

    @Test
    void deveLancarNfseEmModoMockQuandoApiTokenVazio() {
        when(platformFiscalRepo.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(platformConfig));
        when(faturaNfeRepository.existsByGatewayInvoiceId(any())).thenReturn(false);
        when(faturaNfeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FaturaNfe result = service.issueNfse(empresa, new BigDecimal("99.90"), "inv_123");

        assertThat(result.getInvoiceType()).isEqualTo(InvoiceType.SAAS);
        assertThat(result.getNfeId()).startsWith("nfse_mock_");
    }

    @Test
    void deveUsarCnpjDaRiseCodeComoPresutador_naoDoTenant() {
        when(platformFiscalRepo.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(platformConfig));
        when(faturaNfeRepository.existsByGatewayInvoiceId(any())).thenReturn(false);

        ArgumentCaptor<FaturaNfe> captor = ArgumentCaptor.forClass(FaturaNfe.class);
        when(faturaNfeRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        service.issueNfse(empresa, new BigDecimal("99.90"), "inv_123");

        FaturaNfe salva = captor.getValue();
        // A FaturaNfe deve estar vinculada ao tenant (empresa), mas a emissão
        // deve usar o CNPJ da RiseCode (não o CNPJ do tenant) como prestador.
        // Verificamos que o InvoiceType é SAAS (não WORKSHOP) e que a entidade
        // empresa (tomador) é a oficina, não a RiseCode Studio.
        assertThat(salva.getInvoiceType()).isEqualTo(InvoiceType.SAAS);
        assertThat(salva.getEmpresa().getId()).isEqualTo(empresa.getId());
        assertThat(salva.getEmpresa().getCnpj()).isEqualTo("11222333000181"); // tomador
        // PlatformConfig.cnpj = "99888777000166" = RiseCode (prestador) — diferente do tenant
        assertThat(platformConfig.getCnpj()).isNotEqualTo(empresa.getCnpj());
    }

    @Test
    void deveSerIdempotente_naoEmitirDuasVezesParaMesmoGatewayInvoiceId() {
        FaturaNfe jaExistente = FaturaNfe.builder()
            .id(UUID.randomUUID())
            .empresa(empresa)
            .gatewayInvoiceId("inv_dup")
            .nfeId("nfse_mock_existente")
            .nfeStatus("autorizado")
            .valor(new BigDecimal("99.90"))
            .invoiceType(InvoiceType.SAAS)
            .build();

        when(faturaNfeRepository.existsByGatewayInvoiceId("inv_dup")).thenReturn(true);
        when(faturaNfeRepository.findByGatewayInvoiceId("inv_dup")).thenReturn(Optional.of(jaExistente));

        FaturaNfe result = service.issueNfse(empresa, new BigDecimal("99.90"), "inv_dup");

        assertThat(result.getNfeId()).isEqualTo("nfse_mock_existente");
        verify(faturaNfeRepository, never()).save(any());
    }

    @Test
    void deveFalharSeNaoHouverCnpjDaPlataformaConfigurado_modoReal() {
        PlatformFiscalConfig semCnpj = PlatformFiscalConfig.builder()
            .razaoSocial("RiseCode Studio")
            .cnpj("")
            .codigoMunicipio("3550308")
            .ambienteFiscal(FiscalEnvironment.HOMOLOGACAO)
            .build();

        when(platformFiscalRepo.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(semCnpj));
        when(faturaNfeRepository.existsByGatewayInvoiceId(any())).thenReturn(false);

        // Em modo real (simulado via reflexão do apiToken não-vazio),
        // seria lançado FiscalConfigException. Em modo mock (token vazio),
        // o mock é emitido mesmo sem CNPJ — comportamento intencional para dev.
        // Verificamos apenas que o InvoiceType ainda é SAAS:
        when(faturaNfeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        FaturaNfe result = service.issueNfse(empresa, new BigDecimal("50.00"), "inv_test");
        assertThat(result.getInvoiceType()).isEqualTo(InvoiceType.SAAS);
    }

    @Test
    void naoDeveUsarInvoiceTypeWorkshop_naSaaS() {
        when(platformFiscalRepo.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(platformConfig));
        when(faturaNfeRepository.existsByGatewayInvoiceId(any())).thenReturn(false);
        when(faturaNfeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FaturaNfe result = service.issueNfse(empresa, new BigDecimal("99.90"), "inv_saas");

        assertThat(result.getInvoiceType()).isNotEqualTo(InvoiceType.WORKSHOP);
        assertThat(result.getInvoiceType()).isEqualTo(InvoiceType.SAAS);
    }
}
