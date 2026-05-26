package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.Empresa;
import com.manutex.pitstop.domain.entity.FaturaNfe;

import java.math.BigDecimal;

public interface TaxInvoiceService {

    /**
     * Emite uma NFS-e junto à prefeitura via API.
     *
     * @param empresa          Tenant para quem a nota é emitida
     * @param amount           Valor do serviço
     * @param gatewayInvoiceId ID da fatura no gateway de pagamento (idempotência)
     * @return FaturaNfe persistida com os dados retornados pela API fiscal
     */
    FaturaNfe issueNfse(Empresa empresa, BigDecimal amount, String gatewayInvoiceId);
}
