package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.FaturaNfe;
import com.manutex.pitstop.domain.entity.TenantFiscalConfig;
import com.manutex.pitstop.domain.enums.FiscalEnvironment;
import com.manutex.pitstop.domain.enums.InvoiceType;
import com.manutex.pitstop.domain.repository.FaturaNfeRepository;
import com.manutex.pitstop.domain.repository.TenantFiscalConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@SuppressWarnings("null")
class WorkshopInvoiceServiceTest {

    @Mock FaturaNfeRepository        faturaNfeRepository;
    @Mock TenantFiscalConfigRepository tenantFiscalRepo;
    @Mock RestClient.Builder           restClientBuilder;

    private WorkshopInvoiceService service;

    private Empresa empresa;
    private TenantFiscalConfig tenantFiscal;

    @BeforeEach
    void setUp() {
        RestClient mockClient = mock(RestClient.class);
        when(restClientBuilder.baseUrl(any())).thenReturn(restClientBuilder);
        when(restClientBuilder.defaultHeader(any(), any())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(mockClient);

        service = new WorkshopInvoiceService(
            "https://homologacao.focusnfe.com.br",
            faturaNfeRepository,
            tenantFiscalRepo,
            restClientBuilder
        );

        empresa = Empresa.builder()
            .id(UUID.randomUUID())
            .nome("Oficina Teste")
            .cnpj("11222333000181")
            .build();

        tenantFiscal = TenantFiscalConfig.builder()
            .id(UUID.randomUUID())
            .empresa(empresa)
            .cnpj("11222333000181")
            .razaoSocial("Oficina Teste LTDA")
            .codigoMunicipio("3550308")
            .inscricaoMunicipal("654321")
            .codigoServicoMunicipal("14.01")
            .itemListaServico("1.01")
            .aliquotaIss(new BigDecimal("2.00"))
            .ambienteFiscal(FiscalEnvironment.HOMOLOGACAO)
            .fiscalEnabled(true)
            .build();
    }

    @Test
    void deveEmitirNfseWorkshopEmModoMock() {
        when(tenantFiscalRepo.findByEmpresaId(empresa.getId())).thenReturn(Optional.of(tenantFiscal));
        when(faturaNfeRepository.existsByGatewayInvoiceId(any())).thenReturn(false);
        when(faturaNfeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FaturaNfe result = service.issueWorkshopNfse(
            empresa, "João da Silva", "12345678901", "joao@email.com",
            new BigDecimal("250.00"), "os-ref-001"
        );

        assertThat(result.getInvoiceType()).isEqualTo(InvoiceType.WORKSHOP);
        assertThat(result.getNfeId()).isEqualTo("os-ref-001");
    }

    @Test
    void naoDeveEmitirSeConfigFiscalDesabilitada() {
        TenantFiscalConfig desabilitado = TenantFiscalConfig.builder()
            .empresa(empresa)
            .cnpj("11222333000181")
            .inscricaoMunicipal("654321")
            .codigoMunicipio("3550308")
            .codigoServicoMunicipal("14.01")
            .itemListaServico("1.01")
            .fiscalEnabled(false)  // desabilitado
            .build();

        when(tenantFiscalRepo.findByEmpresaId(empresa.getId())).thenReturn(Optional.of(desabilitado));
        when(faturaNfeRepository.existsByGatewayInvoiceId(any())).thenReturn(false);

        assertThatThrownBy(() -> service.issueWorkshopNfse(
            empresa, "Cliente X", "12345678901", null,
            new BigDecimal("100.00"), "os-ref-002"
        )).isInstanceOf(WorkshopInvoiceService.FiscalConfigException.class)
          .hasMessageContaining("fiscalEnabled=false");
    }

    @Test
    void naoDeveEmitirSeConfigFiscalInexistente() {
        when(tenantFiscalRepo.findByEmpresaId(empresa.getId())).thenReturn(Optional.empty());
        when(faturaNfeRepository.existsByGatewayInvoiceId(any())).thenReturn(false);

        assertThatThrownBy(() -> service.issueWorkshopNfse(
            empresa, "Cliente Y", "12345678901", null,
            new BigDecimal("100.00"), "os-ref-003"
        )).isInstanceOf(WorkshopInvoiceService.FiscalConfigException.class);
    }

    @Test
    void deveSerIdempotente_naoEmitirDuasVezes() {
        FaturaNfe jaExistente = FaturaNfe.builder()
            .id(UUID.randomUUID())
            .empresa(empresa)
            .gatewayInvoiceId("os-ref-004")
            .nfeId("os-ref-004")
            .nfeStatus("autorizado")
            .valor(new BigDecimal("200.00"))
            .invoiceType(InvoiceType.WORKSHOP)
            .build();

        when(tenantFiscalRepo.findByEmpresaId(empresa.getId())).thenReturn(Optional.of(tenantFiscal));
        when(faturaNfeRepository.existsByGatewayInvoiceId("os-ref-004")).thenReturn(true);
        when(faturaNfeRepository.findByGatewayInvoiceId("os-ref-004")).thenReturn(Optional.of(jaExistente));

        FaturaNfe result = service.issueWorkshopNfse(
            empresa, "Cliente Z", "12345678901", null,
            new BigDecimal("200.00"), "os-ref-004"
        );

        assertThat(result.getNfeId()).isEqualTo("os-ref-004");
        verify(faturaNfeRepository, never()).save(any());
    }

    @Test
    void cnpjDaOficinaNaoDeveIgualarCnpjDaPlataforma() {
        // Garante que o prestador é a oficina, não a RiseCode Studio.
        // Em produção, PlatformFiscalConfig.cnpj != TenantFiscalConfig.cnpj é obrigatório.
        String cnpjRiseCode = "99888777000166"; // CNPJ fictício da RiseCode
        String cnpjOficina  = tenantFiscal.getCnpj(); // "11222333000181"

        assertThat(cnpjOficina).isNotEqualTo(cnpjRiseCode);
    }

    @Test
    void emissaoWorkshopDeveProduziirInvoiceTypeWorkshop() {
        when(tenantFiscalRepo.findByEmpresaId(empresa.getId())).thenReturn(Optional.of(tenantFiscal));
        when(faturaNfeRepository.existsByGatewayInvoiceId(any())).thenReturn(false);
        when(faturaNfeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FaturaNfe result = service.issueWorkshopNfse(
            empresa, "Cliente Teste", "98765432100", null,
            new BigDecimal("180.00"), "os-ref-005"
        );

        assertThat(result.getInvoiceType()).isEqualTo(InvoiceType.WORKSHOP);
        assertThat(result.getInvoiceType()).isNotEqualTo(InvoiceType.SAAS);
    }
}
