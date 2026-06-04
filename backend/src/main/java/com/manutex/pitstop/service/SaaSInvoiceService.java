package com.manutex.pitstop.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.manutex.pitstop.config.BillingProperties;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.FaturaNfe;
import com.manutex.pitstop.domain.entity.PlatformFiscalConfig;
import com.manutex.pitstop.domain.entity.TenantFiscalConfig;
import com.manutex.pitstop.domain.enums.FiscalEnvironment;
import com.manutex.pitstop.domain.enums.InvoiceType;
import com.manutex.pitstop.domain.repository.FaturaNfeRepository;
import com.manutex.pitstop.domain.repository.PlatformFiscalConfigRepository;
import com.manutex.pitstop.domain.repository.TenantFiscalConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Emite NFS-e da assinatura SaaS do PitStop Manager.
 *
 * PRESTADOR = RiseCode Studio (dados de PlatformFiscalConfig)
 * TOMADOR   = Oficina/empresa assinante (dados de Empresa + TenantFiscalConfig)
 *
 * Nunca usar o CNPJ da oficina como prestador neste fluxo.
 */
@Slf4j
@Service
public class SaaSInvoiceService implements TaxInvoiceService {

    private static final String NFSE_PATH = "/v2/nfse";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestClient restClient;
    private final FaturaNfeRepository faturaNfeRepository;
    private final PlatformFiscalConfigRepository platformFiscalRepo;
    private final TenantFiscalConfigRepository tenantFiscalRepo;
    private final BillingProperties billingProperties;

    @Value("${billing.nfe.api-token:}")
    private String apiToken;

    @SuppressWarnings("null") // @Value com default nunca injeta null; falso positivo do checker
    public SaaSInvoiceService(
        @Value("${billing.nfe.api-url:https://homologacao.focusnfe.com.br}") String apiUrl,
        FaturaNfeRepository faturaNfeRepository,
        PlatformFiscalConfigRepository platformFiscalRepo,
        TenantFiscalConfigRepository tenantFiscalRepo,
        BillingProperties billingProperties,
        RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder
            .baseUrl(apiUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.faturaNfeRepository     = faturaNfeRepository;
        this.platformFiscalRepo      = platformFiscalRepo;
        this.tenantFiscalRepo        = tenantFiscalRepo;
        this.billingProperties       = billingProperties;
    }

    @Override
    public FaturaNfe issueNfse(Empresa empresa, BigDecimal amount, String gatewayInvoiceId) {
        // Idempotência: não emite se já existe fatura para este invoiceId
        if (gatewayInvoiceId != null && faturaNfeRepository.existsByGatewayInvoiceId(gatewayInvoiceId)) {
            log.info("NFS-e já emitida para gatewayInvoiceId={}, ignorando", gatewayInvoiceId);
            return faturaNfeRepository.findByGatewayInvoiceId(gatewayInvoiceId)
                .orElseThrow(() -> new IllegalStateException("FaturaNfe não encontrada: " + gatewayInvoiceId));
        }

        PlatformFiscalConfig platform = resolvePlatformConfig();

        if (apiToken.isBlank()) {
            return emitirMock(empresa, amount, gatewayInvoiceId, platform);
        }

        validatePlatformForEmission(platform);
        return emitirViaApi(empresa, amount, gatewayInvoiceId, platform);
    }

    // ── Emissão real via Focus NFe ────────────────────────────────────────────

    @SuppressWarnings("null")
    private FaturaNfe emitirViaApi(Empresa empresa, BigDecimal amount,
                                   String gatewayInvoiceId, PlatformFiscalConfig platform) {
        String referencia = "pitstop-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Map<String, Object> payload = buildNfsePayload(empresa, amount, referencia, platform);

        try {
            FocusNfseResponse response = restClient.post()
                .uri(NFSE_PATH + "?ref=" + referencia)
                .headers(h -> h.setBasicAuth(apiToken, ""))
                .body(payload)
                .retrieve()
                .body(FocusNfseResponse.class);

            log.info("NFS-e SaaS emitida: ref={} status={} prestador_cnpj={}",
                referencia,
                response != null ? response.status() : "N/A",
                mask(platform.getCnpj()));

            return faturaNfeRepository.save(FaturaNfe.builder()
                .empresa(empresa)
                .gatewayInvoiceId(gatewayInvoiceId)
                .nfeId(referencia)
                .nfeStatus(response != null ? response.status() : "processando")
                .valor(amount)
                .invoiceType(InvoiceType.SAAS)
                .pdfUrl(response != null ? response.caminhoPdf() : null)
                .xmlUrl(response != null ? response.caminhoXml() : null)
                .build());

        } catch (RestClientException e) {
            log.error("Falha ao emitir NFS-e SaaS para empresa={}: {}", empresa.getId(), e.getMessage());
            return faturaNfeRepository.save(FaturaNfe.builder()
                .empresa(empresa)
                .gatewayInvoiceId(gatewayInvoiceId)
                .nfeId(referencia)
                .nfeStatus("erro_emissao")
                .valor(amount)
                .invoiceType(InvoiceType.SAAS)
                .build());
        }
    }

    // ── Mock para desenvolvimento ─────────────────────────────────────────────

    @SuppressWarnings("null")
    private FaturaNfe emitirMock(Empresa empresa, BigDecimal amount,
                                  String gatewayInvoiceId, PlatformFiscalConfig platform) {
        String mockNfeId = "nfse_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("[SAAS-INVOICE-MOCK] prestador={} tomador={} valor={} ref={}",
            mask(platform.getCnpj()), mask(empresa.getCnpj()), amount, mockNfeId);

        return faturaNfeRepository.save(FaturaNfe.builder()
            .empresa(empresa)
            .gatewayInvoiceId(gatewayInvoiceId)
            .nfeId(mockNfeId)
            .nfeStatus("autorizado")
            .valor(amount)
            .invoiceType(InvoiceType.SAAS)
            .pdfUrl("https://homologacao.focusnfe.com.br/nota_fiscal_servico/" + mockNfeId + ".pdf")
            .xmlUrl("https://homologacao.focusnfe.com.br/nota_fiscal_servico/" + mockNfeId + ".xml")
            .build());
    }

    // ── Montagem do payload Focus NFe ─────────────────────────────────────────

    private Map<String, Object> buildNfsePayload(Empresa empresa, BigDecimal amount,
                                                  String referencia, PlatformFiscalConfig platform) {
        // PRESTADOR = RiseCode Studio
        Map<String, Object> prestador = new LinkedHashMap<>();
        prestador.put("cnpj", cleanDigits(platform.getCnpj()));
        prestador.put("codigo_municipio", platform.getCodigoMunicipio());
        if (platform.getInscricaoMunicipal() != null) {
            prestador.put("inscricao_municipal", platform.getInscricaoMunicipal());
        }

        // TOMADOR = Oficina assinante
        Map<String, Object> tomador = buildTomador(empresa);

        // SERVIÇO
        String aliquota = platform.getAliquotaIss() != null
            ? platform.getAliquotaIss().toPlainString() : "2.00";
        String itemLista = platform.getItemListaServico() != null
            ? platform.getItemListaServico() : "1.01";
        String codigoServico = platform.getCodigoServicoMunicipal() != null
            ? platform.getCodigoServicoMunicipal() : "14.01";

        Map<String, Object> servico = new LinkedHashMap<>();
        servico.put("aliquota", aliquota);
        servico.put("base_calculo", amount.toPlainString());
        servico.put("codigo_municipio", platform.getCodigoMunicipio());
        servico.put("codigo_tributacao_municipio", codigoServico);
        servico.put("descricao", "Assinatura mensal SaaS - PitStop Manager - Ref.: " + referencia);
        servico.put("discriminacao",
            "Serviço de Software como Serviço (SaaS) - Gestão de Ordens de Serviço Automotivo");
        servico.put("iss_retido", false);
        servico.put("item_lista_servico", itemLista);
        servico.put("valor_servicos", amount.toPlainString());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prestador", prestador);
        payload.put("tomador", tomador);
        payload.put("servico", servico);
        payload.put("data_emissao", LocalDate.now().format(DATE_FMT));
        payload.put("numero_rps", referencia.replace("pitstop-", ""));
        payload.put("serie_rps", "1");
        payload.put("tipo_rps", "RPS");

        return payload;
    }

    private Map<String, Object> buildTomador(Empresa empresa) {
        TenantFiscalConfig tenantFiscal = tenantFiscalRepo.findByEmpresaId(empresa.getId()).orElse(null);

        String cnpj = tenantFiscal != null && tenantFiscal.getCnpj() != null
            ? tenantFiscal.getCnpj() : empresa.getCnpj();
        String razaoSocial = tenantFiscal != null && tenantFiscal.getRazaoSocial() != null
            ? tenantFiscal.getRazaoSocial()
            : (empresa.getRazaoSocial() != null ? empresa.getRazaoSocial() : empresa.getNome());

        Map<String, Object> tomador = new LinkedHashMap<>();
        tomador.put("cnpj", cleanDigits(cnpj));
        tomador.put("razao_social", razaoSocial);

        String logradouro = tenantFiscal != null && tenantFiscal.getEndereco() != null
            ? tenantFiscal.getEndereco() : empresa.getLogradouro();
        if (logradouro != null) {
            Map<String, Object> endereco = new LinkedHashMap<>();
            endereco.put("logradouro", logradouro);

            String numero = tenantFiscal != null && tenantFiscal.getNumero() != null
                ? tenantFiscal.getNumero() : empresa.getNumero();
            if (numero != null) endereco.put("numero", numero);

            String bairro = tenantFiscal != null && tenantFiscal.getBairro() != null
                ? tenantFiscal.getBairro() : empresa.getBairro();
            if (bairro != null) endereco.put("bairro", bairro);

            String codMun = tenantFiscal != null && tenantFiscal.getCodigoMunicipio() != null
                ? tenantFiscal.getCodigoMunicipio() : empresa.getCodigoMunicipioIbge();
            if (codMun != null) endereco.put("codigo_municipio", codMun);

            String uf = tenantFiscal != null && tenantFiscal.getUf() != null
                ? tenantFiscal.getUf() : empresa.getUf();
            if (uf != null) endereco.put("uf", uf);

            String cep = tenantFiscal != null && tenantFiscal.getCep() != null
                ? tenantFiscal.getCep() : empresa.getCep();
            if (cep != null) endereco.put("cep", cleanDigits(cep));

            tomador.put("endereco", endereco);
        }

        String emailFiscal = tenantFiscal != null && tenantFiscal.getEmailFiscal() != null
            ? tenantFiscal.getEmailFiscal() : empresa.getEmailFiscal();
        if (emailFiscal != null) tomador.put("email", emailFiscal);

        String telefone = tenantFiscal != null && tenantFiscal.getTelefoneFiscal() != null
            ? tenantFiscal.getTelefoneFiscal() : empresa.getTelefoneFiscal();
        if (telefone != null) {
            String tel = cleanDigits(telefone);
            if (tel.length() >= 10) {
                tomador.put("telefone", Map.of("ddd", tel.substring(0, 2), "numero", tel.substring(2)));
            }
        }

        return tomador;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PlatformFiscalConfig resolvePlatformConfig() {
        return platformFiscalRepo.findFirstByOrderByUpdatedAtDesc()
            .orElseGet(this::buildFallbackFromProperties);
    }

    private PlatformFiscalConfig buildFallbackFromProperties() {
        BillingProperties.PlatformFiscal pf = billingProperties.platformFiscal();
        if (pf == null) return emptyPlatformConfig();

        log.warn("[FISCAL] PlatformFiscalConfig não encontrada no banco — usando fallback de variáveis de ambiente");
        return PlatformFiscalConfig.builder()
            .cnpj(pf.cnpj() != null ? pf.cnpj() : "")
            .razaoSocial(pf.razaoSocial() != null ? pf.razaoSocial() : "RiseCode Studio")
            .codigoMunicipio(pf.codigoMunicipio() != null ? pf.codigoMunicipio() : "3550308")
            .inscricaoMunicipal(pf.inscricaoMunicipal())
            .codigoServicoMunicipal(pf.codigoServicoMunicipal())
            .itemListaServico(pf.itemListaServico() != null ? pf.itemListaServico() : "1.01")
            .aliquotaIss(pf.aliquotaIss() != null ? new BigDecimal(pf.aliquotaIss()) : new BigDecimal("2.00"))
            .ambienteFiscal(FiscalEnvironment.HOMOLOGACAO)
            .build();
    }

    private PlatformFiscalConfig emptyPlatformConfig() {
        return PlatformFiscalConfig.builder()
            .cnpj("")
            .razaoSocial("RiseCode Studio")
            .codigoMunicipio("3550308")
            .aliquotaIss(new BigDecimal("2.00"))
            .ambienteFiscal(FiscalEnvironment.HOMOLOGACAO)
            .build();
    }

    private void validatePlatformForEmission(PlatformFiscalConfig platform) {
        if (platform.getCnpj() == null || platform.getCnpj().isBlank()) {
            throw new FiscalConfigException(
                "CNPJ da RiseCode Studio não configurado. " +
                "Configure PLATFORM_FISCAL_CNPJ ou cadastre via /api/v1/admin/fiscal/platform.");
        }
        if (platform.getCodigoMunicipio() == null || platform.getCodigoMunicipio().isBlank()) {
            throw new FiscalConfigException("Código IBGE do município prestador não configurado.");
        }
    }

    private static String cleanDigits(String value) {
        return value != null ? value.replaceAll("[^0-9]", "") : "";
    }

    /** Mascara um CNPJ/CPF para logs — exibe apenas os 4 últimos dígitos. */
    private static String mask(String value) {
        if (value == null || value.isBlank()) return "***";
        String digits = cleanDigits(value);
        if (digits.length() < 4) return "***";
        return "***" + digits.substring(digits.length() - 4);
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    public static class FiscalConfigException extends RuntimeException {
        public FiscalConfigException(String message) { super(message); }
    }

    // ── Response record ──────────────────────────────────────────────────────

    private record FocusNfseResponse(
        String status,
        @JsonProperty("caminho_pdf_nota_fiscal") String caminhoPdf,
        @JsonProperty("caminho_xml_nota_fiscal") String caminhoXml,
        @JsonProperty("numero") String numero
    ) {}
}
