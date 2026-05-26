package com.manutex.pitstop.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.manutex.pitstop.web.dto.CnpjLookupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Consulta dados cadastrais de um CNPJ via BrasilAPI.
 * Endpoint: GET https://brasilapi.com.br/api/cnpj/v1/{cnpj}
 *
 * Não requer chave de API. Em caso de falha (CNPJ inexistente, API indisponível),
 * lança CnpjNotFoundException para tratamento adequado no controller.
 */
@Slf4j
@Service
public class CnpjLookupService {

    private static final String BRASIL_API_URL = "https://brasilapi.com.br";
    private static final String CNPJ_PATH = "/api/cnpj/v1/{cnpj}";

    private final RestClient restClient;

    public CnpjLookupService(RestClient.Builder builder) {
        this.restClient = builder
            .baseUrl(BRASIL_API_URL)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("User-Agent", "PitStop-Manager/1.0")
            .build();
    }

    public CnpjLookupResponse lookup(String cnpj) {
        String cnpjDigits = cnpj.replaceAll("[^0-9]", "");
        if (cnpjDigits.length() != 14) {
            throw new CnpjNotFoundException("CNPJ deve ter 14 dígitos: " + cnpj);
        }

        try {
            BrasilApiCnpjResponse response = restClient.get()
                .uri(CNPJ_PATH, cnpjDigits)
                .retrieve()
                .body(BrasilApiCnpjResponse.class);

            if (response == null) {
                throw new CnpjNotFoundException("CNPJ não encontrado na Receita Federal: " + cnpj);
            }

            log.info("CNPJ lookup bem-sucedido: {} — {}", cnpjDigits, response.razaoSocial());
            return toResponse(response);

        } catch (RestClientResponseException e) {
            log.warn("BrasilAPI retornou {} para CNPJ {}: {}", e.getStatusCode(), cnpjDigits, e.getMessage());
            throw new CnpjNotFoundException("CNPJ não localizado ou inativo: " + cnpj);
        } catch (CnpjNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao consultar BrasilAPI para CNPJ {}: {}", cnpjDigits, e.getMessage());
            throw new CnpjNotFoundException("Serviço de consulta de CNPJ indisponível. Tente novamente.");
        }
    }

    private CnpjLookupResponse toResponse(BrasilApiCnpjResponse r) {
        String telefone = r.dddTelefone1() != null
            ? r.dddTelefone1().replaceAll("[^0-9]", "")
            : null;

        return new CnpjLookupResponse(
            formatCnpj(r.cnpj()),
            r.razaoSocial(),
            r.nomeFantasia(),
            r.cep() != null ? r.cep().replaceAll("[^0-9]", "") : null,
            r.logradouro(),
            r.numero(),
            r.complemento(),
            r.bairro(),
            r.municipio(),
            r.uf(),
            r.codigoMunicipioIbge() != null ? r.codigoMunicipioIbge().toString() : null,
            r.email(),
            telefone,
            r.descricaoSituacaoCadastral()
        );
    }

    private String formatCnpj(String digits) {
        if (digits == null || digits.length() != 14) return digits;
        return digits.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    // ── BrasilAPI response record ─────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BrasilApiCnpjResponse(
        String cnpj,
        @JsonProperty("razao_social")                    String razaoSocial,
        @JsonProperty("nome_fantasia")                   String nomeFantasia,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String municipio,
        String uf,
        String cep,
        String email,
        @JsonProperty("ddd_telefone_1")                  String dddTelefone1,
        @JsonProperty("descricao_situacao_cadastral")    String descricaoSituacaoCadastral,
        @JsonProperty("codigo_municipio_ibge")           Integer codigoMunicipioIbge
    ) {}

    public static class CnpjNotFoundException extends RuntimeException {
        public CnpjNotFoundException(String message) { super(message); }
    }
}
