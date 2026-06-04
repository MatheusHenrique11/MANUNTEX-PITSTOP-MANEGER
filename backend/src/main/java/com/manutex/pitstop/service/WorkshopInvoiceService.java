package com.manutex.pitstop.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.FaturaNfe;
import com.manutex.pitstop.domain.entity.TenantFiscalConfig;
import com.manutex.pitstop.domain.enums.InvoiceType;
import com.manutex.pitstop.domain.repository.FaturaNfeRepository;
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
 * Emite NFS-e de serviços automotivos da oficina para o cliente final.
 *
 * PRESTADOR = Oficina/tenant (dados de TenantFiscalConfig)
 * TOMADOR   = Cliente final da oficina (nome, CPF/CNPJ)
 *
 * Nunca usar o CNPJ da RiseCode Studio como prestador neste fluxo.
 * Somente emite se TenantFiscalConfig.fiscalEnabled = true e dados estiverem completos.
 */
@Slf4j
@Service
public class WorkshopInvoiceService {

    private static final String NFSE_PATH = "/v2/nfse";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestClient restClient;
    private final FaturaNfeRepository faturaNfeRepository;
    private final TenantFiscalConfigRepository tenantFiscalRepo;

    @Value("${billing.nfe.api-token:}")
    private String apiToken;

    @SuppressWarnings("null")
    public WorkshopInvoiceService(
        @Value("${billing.nfe.api-url:https://homologacao.focusnfe.com.br}") String apiUrl,
        FaturaNfeRepository faturaNfeRepository,
        TenantFiscalConfigRepository tenantFiscalRepo,
        RestClient.Builder restClientBuilder
    ) {
        this.restClient       = restClientBuilder
            .baseUrl(apiUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.faturaNfeRepository = faturaNfeRepository;
        this.tenantFiscalRepo    = tenantFiscalRepo;
    }

    /**
     * Emite NFS-e de serviço automotivo.
     *
     * @param empresa         Empresa (oficina) que presta o serviço
     * @param tomadorNome     Nome/razão social do cliente final
     * @param tomadorCpfCnpj  CPF ou CNPJ do cliente final
     * @param tomadorEmail    E-mail do cliente final (opcional)
     * @param amount          Valor do serviço
     * @param osReferencia    Referência da OS para idempotência (ex: "os-<uuid>")
     */
    public FaturaNfe issueWorkshopNfse(
        Empresa empresa,
        String tomadorNome,
        String tomadorCpfCnpj,
        String tomadorEmail,
        BigDecimal amount,
        String osReferencia
    ) {
        TenantFiscalConfig tenantFiscal = tenantFiscalRepo.findByEmpresaId(empresa.getId())
            .orElseThrow(() -> new FiscalConfigException(
                "Configuração fiscal não encontrada para empresa: " + empresa.getId()));

        if (!tenantFiscal.isReadyForEmission()) {
            throw new FiscalConfigException(
                "Configuração fiscal da empresa '" + empresa.getNome() + "' está incompleta " +
                "ou emissão não está habilitada (fiscalEnabled=false).");
        }

        // Idempotência por referência da OS
        if (osReferencia != null && faturaNfeRepository.existsByGatewayInvoiceId(osReferencia)) {
            log.info("NFS-e Workshop já emitida para osReferencia={}, ignorando", osReferencia);
            return faturaNfeRepository.findByGatewayInvoiceId(osReferencia)
                .orElseThrow(() -> new IllegalStateException("FaturaNfe não encontrada: " + osReferencia));
        }

        if (apiToken.isBlank()) {
            return emitirMock(empresa, tenantFiscal, tomadorNome, amount, osReferencia);
        }

        return emitirViaApi(empresa, tenantFiscal, tomadorNome, tomadorCpfCnpj, tomadorEmail, amount, osReferencia);
    }

    @SuppressWarnings("null")
    private FaturaNfe emitirViaApi(
        Empresa empresa, TenantFiscalConfig fiscal,
        String tomadorNome, String tomadorCpfCnpj, String tomadorEmail,
        BigDecimal amount, String osReferencia
    ) {
        String referencia = osReferencia != null ? osReferencia
            : "ws-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Map<String, Object> payload = buildNfsePayload(
            fiscal, tomadorNome, tomadorCpfCnpj, tomadorEmail, amount, referencia);

        try {
            FocusNfseResponse response = restClient.post()
                .uri(NFSE_PATH + "?ref=" + referencia)
                .headers(h -> h.setBasicAuth(apiToken, ""))
                .body(payload)
                .retrieve()
                .body(FocusNfseResponse.class);

            log.info("NFS-e Workshop emitida: ref={} status={} prestador_cnpj={}",
                referencia,
                response != null ? response.status() : "N/A",
                mask(fiscal.getCnpj()));

            return faturaNfeRepository.save(FaturaNfe.builder()
                .empresa(empresa)
                .gatewayInvoiceId(referencia)
                .nfeId(referencia)
                .nfeStatus(response != null ? response.status() : "processando")
                .valor(amount)
                .invoiceType(InvoiceType.WORKSHOP)
                .pdfUrl(response != null ? response.caminhoPdf() : null)
                .xmlUrl(response != null ? response.caminhoXml() : null)
                .build());

        } catch (RestClientException e) {
            log.error("Falha ao emitir NFS-e Workshop para empresa={}: {}", empresa.getId(), e.getMessage());
            return faturaNfeRepository.save(FaturaNfe.builder()
                .empresa(empresa)
                .gatewayInvoiceId(referencia)
                .nfeId(referencia)
                .nfeStatus("erro_emissao")
                .valor(amount)
                .invoiceType(InvoiceType.WORKSHOP)
                .build());
        }
    }

    @SuppressWarnings("null")
    private FaturaNfe emitirMock(Empresa empresa, TenantFiscalConfig fiscal,
                                  String tomadorNome, BigDecimal amount, String osReferencia) {
        String mockNfeId = osReferencia != null ? osReferencia
            : "ws_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("[WORKSHOP-INVOICE-MOCK] prestador_empresa={} tomador={} valor={} ref={}",
            empresa.getId(), tomadorNome, amount, mockNfeId);

        return faturaNfeRepository.save(FaturaNfe.builder()
            .empresa(empresa)
            .gatewayInvoiceId(mockNfeId)
            .nfeId(mockNfeId)
            .nfeStatus("autorizado")
            .valor(amount)
            .invoiceType(InvoiceType.WORKSHOP)
            .pdfUrl("https://homologacao.focusnfe.com.br/nota_fiscal_servico/" + mockNfeId + ".pdf")
            .xmlUrl("https://homologacao.focusnfe.com.br/nota_fiscal_servico/" + mockNfeId + ".xml")
            .build());
    }

    private Map<String, Object> buildNfsePayload(
        TenantFiscalConfig fiscal,
        String tomadorNome, String tomadorCpfCnpj, String tomadorEmail,
        BigDecimal amount, String referencia
    ) {
        // PRESTADOR = oficina
        Map<String, Object> prestador = new LinkedHashMap<>();
        prestador.put("cnpj", cleanDigits(fiscal.getCnpj()));
        prestador.put("codigo_municipio", fiscal.getCodigoMunicipio());
        if (fiscal.getInscricaoMunicipal() != null) {
            prestador.put("inscricao_municipal", fiscal.getInscricaoMunicipal());
        }

        // TOMADOR = cliente final
        Map<String, Object> tomador = new LinkedHashMap<>();
        String digits = cleanDigits(tomadorCpfCnpj);
        tomador.put(digits.length() == 11 ? "cpf" : "cnpj", digits);
        tomador.put("razao_social", tomadorNome);
        if (tomadorEmail != null && !tomadorEmail.isBlank()) {
            tomador.put("email", tomadorEmail);
        }

        String aliquota = fiscal.getAliquotaIss() != null
            ? fiscal.getAliquotaIss().toPlainString() : "2.00";
        String itemLista = fiscal.getItemListaServico() != null
            ? fiscal.getItemListaServico() : "1.01";
        String codigoServico = fiscal.getCodigoServicoMunicipal() != null
            ? fiscal.getCodigoServicoMunicipal() : "14.01";

        Map<String, Object> servico = new LinkedHashMap<>();
        servico.put("aliquota", aliquota);
        servico.put("base_calculo", amount.toPlainString());
        servico.put("codigo_municipio", fiscal.getCodigoMunicipio());
        servico.put("codigo_tributacao_municipio", codigoServico);
        servico.put("descricao", "Serviços automotivos - Ref.: " + referencia);
        servico.put("discriminacao", "Manutenção e serviços automotivos");
        servico.put("iss_retido", false);
        servico.put("item_lista_servico", itemLista);
        servico.put("valor_servicos", amount.toPlainString());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prestador", prestador);
        payload.put("tomador", tomador);
        payload.put("servico", servico);
        payload.put("data_emissao", LocalDate.now().format(DATE_FMT));
        payload.put("numero_rps", referencia.replace("ws-", "").replace("ws_mock_", ""));
        payload.put("serie_rps", "1");
        payload.put("tipo_rps", "RPS");

        return payload;
    }

    private static String cleanDigits(String value) {
        return value != null ? value.replaceAll("[^0-9]", "") : "";
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) return "***";
        String digits = cleanDigits(value);
        if (digits.length() < 4) return "***";
        return "***" + digits.substring(digits.length() - 4);
    }

    public static class FiscalConfigException extends RuntimeException {
        public FiscalConfigException(String message) { super(message); }
    }

    private record FocusNfseResponse(
        String status,
        @JsonProperty("caminho_pdf_nota_fiscal") String caminhoPdf,
        @JsonProperty("caminho_xml_nota_fiscal") String caminhoXml,
        @JsonProperty("numero") String numero
    ) {}
}
