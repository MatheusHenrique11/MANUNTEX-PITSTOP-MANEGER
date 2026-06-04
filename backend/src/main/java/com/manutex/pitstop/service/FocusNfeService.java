package com.manutex.pitstop.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.FaturaNfe;
import com.manutex.pitstop.domain.repository.FaturaNfeRepository;
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
 * @deprecated Substituído por {@link SaaSInvoiceService} (prestador correto: RiseCode Studio)
 * e {@link WorkshopInvoiceService} (prestador: oficina tenant).
 * Esta classe foi mantida apenas como referência histórica e não é mais registrada como @Service.
 */
@Slf4j
@Deprecated(since = "V10", forRemoval = true)
public class FocusNfeService {

    private static final String NFSE_PATH = "/v2/nfse";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestClient restClient;
    private final FaturaNfeRepository faturaNfeRepository;

    @Value("${billing.nfe.api-token:}")
    private String apiToken;

    @Value("${billing.nfe.municipio-prestador:3550308}")
    private String municipioPrestador;

    public FocusNfeService(
        @Value("${billing.nfe.api-url:https://homologacao.focusnfe.com.br}") String apiUrl,
        FaturaNfeRepository faturaNfeRepository,
        RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder
            .baseUrl(apiUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.faturaNfeRepository = faturaNfeRepository;
    }

    public FaturaNfe issueNfse(Empresa empresa, BigDecimal amount, String gatewayInvoiceId) {
        // Idempotência: não emite se já existe fatura para este invoiceId
        if (gatewayInvoiceId != null && faturaNfeRepository.existsByGatewayInvoiceId(gatewayInvoiceId)) {
            log.info("NFS-e já emitida para gatewayInvoiceId={}, ignorando", gatewayInvoiceId);
            return faturaNfeRepository.findByGatewayInvoiceId(gatewayInvoiceId)
                .orElseThrow(() -> new IllegalStateException("FaturaNfe não encontrada: " + gatewayInvoiceId));
        }

        if (apiToken.isBlank()) {
            return emitirMock(empresa, amount, gatewayInvoiceId);
        }

        return emitirViaApi(empresa, amount, gatewayInvoiceId);
    }

    @SuppressWarnings("null") // RestClient/JpaRepository são @NonNull por contrato; falso positivo do checker do Eclipse
    private FaturaNfe emitirViaApi(Empresa empresa, BigDecimal amount, String gatewayInvoiceId) {
        String referencia = "pitstop-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Map<String, Object> payload = buildNfsePayload(empresa, amount, referencia);

        try {
            FocusNfseResponse response = restClient.post()
                .uri(NFSE_PATH + "?ref=" + referencia)
                .headers(h -> h.setBasicAuth(apiToken, ""))
                .body(payload)
                .retrieve()
                .body(FocusNfseResponse.class);

            log.info("NFS-e emitida via Focus NFe: ref={} status={}", referencia,
                     response != null ? response.status() : "N/A");

            FaturaNfe fatura = FaturaNfe.builder()
                .empresa(empresa)
                .gatewayInvoiceId(gatewayInvoiceId)
                .nfeId(referencia)
                .nfeStatus(response != null ? response.status() : "processando")
                .valor(amount)
                .pdfUrl(response != null ? response.caminhoPdf() : null)
                .xmlUrl(response != null ? response.caminhoXml() : null)
                .build();

            return faturaNfeRepository.save(fatura);

        } catch (RestClientException e) {
            log.error("Falha ao emitir NFS-e via Focus NFe para empresa={}: {}", empresa.getId(), e.getMessage());
            FaturaNfe fatura = FaturaNfe.builder()
                .empresa(empresa)
                .gatewayInvoiceId(gatewayInvoiceId)
                .nfeId(referencia)
                .nfeStatus("erro_emissao")
                .valor(amount)
                .build();
            return faturaNfeRepository.save(fatura);
        }
    }

    @SuppressWarnings("null")
    private FaturaNfe emitirMock(Empresa empresa, BigDecimal amount, String gatewayInvoiceId) {
        String mockNfeId = "nfse_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("[FOCUS-NFE-MOCK] Emitindo NFS-e para empresa={} valor={} → {}", empresa.getId(), amount, mockNfeId);

        FaturaNfe fatura = FaturaNfe.builder()
            .empresa(empresa)
            .gatewayInvoiceId(gatewayInvoiceId)
            .nfeId(mockNfeId)
            .nfeStatus("autorizado")
            .valor(amount)
            .pdfUrl("https://homologacao.focusnfe.com.br/nota_fiscal_servico/" + mockNfeId + ".pdf")
            .xmlUrl("https://homologacao.focusnfe.com.br/nota_fiscal_servico/" + mockNfeId + ".xml")
            .build();

        return faturaNfeRepository.save(fatura);
    }

    private Map<String, Object> buildNfsePayload(Empresa empresa, BigDecimal amount, String referencia) {
        Map<String, Object> tomador = new LinkedHashMap<>();
        tomador.put("cnpj", empresa.getCnpj().replaceAll("[^0-9]", ""));
        tomador.put("razao_social", empresa.getRazaoSocial() != null ? empresa.getRazaoSocial() : empresa.getNome());

        if (empresa.getLogradouro() != null) {
            Map<String, Object> endereco = new LinkedHashMap<>();
            endereco.put("logradouro", empresa.getLogradouro());
            if (empresa.getNumero() != null)               endereco.put("numero", empresa.getNumero());
            if (empresa.getComplemento() != null)          endereco.put("complemento", empresa.getComplemento());
            if (empresa.getBairro() != null)               endereco.put("bairro", empresa.getBairro());
            if (empresa.getCodigoMunicipioIbge() != null)  endereco.put("codigo_municipio", empresa.getCodigoMunicipioIbge());
            if (empresa.getUf() != null)                   endereco.put("uf", empresa.getUf());
            if (empresa.getCep() != null)                  endereco.put("cep", empresa.getCep().replaceAll("[^0-9]", ""));
            tomador.put("endereco", endereco);
        }

        if (empresa.getTelefoneFiscal() != null) {
            String tel = empresa.getTelefoneFiscal().replaceAll("[^0-9]", "");
            if (tel.length() >= 10) {
                tomador.put("telefone", Map.of("ddd", tel.substring(0, 2), "numero", tel.substring(2)));
            }
        }

        if (empresa.getEmailFiscal() != null) {
            tomador.put("email", empresa.getEmailFiscal());
        }

        Map<String, Object> servico = new LinkedHashMap<>();
        servico.put("aliquota", "2.00");
        servico.put("base_calculo", amount.toPlainString());
        servico.put("codigo_municipio", municipioPrestador);
        servico.put("codigo_tributacao_municipio", "14.01");
        servico.put("descricao", "Assinatura mensal SaaS - PitStop Manager - Ref.: " + referencia);
        servico.put("discriminacao", "Serviço de Software como Serviço (SaaS) - Gestão de Ordens de Serviço Automotivo");
        servico.put("iss_retido", false);
        servico.put("item_lista_servico", "1.01");
        servico.put("valor_servicos", amount.toPlainString());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prestador", Map.of("cnpj", empresa.getCnpj().replaceAll("[^0-9]", ""), "codigo_municipio", municipioPrestador));
        payload.put("tomador", tomador);
        payload.put("servico", servico);
        payload.put("data_emissao", LocalDate.now().format(DATE_FMT));
        payload.put("numero_rps", referencia.replace("pitstop-", ""));
        payload.put("serie_rps", "1");
        payload.put("tipo_rps", "RPS");

        return payload;
    }

    // ── Response record ──────────────────────────────────────────────────────

    private record FocusNfseResponse(
        String status,
        @JsonProperty("caminho_pdf_nota_fiscal") String caminhoPdf,
        @JsonProperty("caminho_xml_nota_fiscal") String caminhoXml,
        @JsonProperty("numero") String numero
    ) {}
}
