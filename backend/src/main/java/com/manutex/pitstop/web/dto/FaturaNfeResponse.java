package com.manutex.pitstop.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FaturaNfeResponse(
    UUID id,
    String gatewayInvoiceId,
    String nfeId,
    String nfeStatus,
    BigDecimal valor,
    String pdfUrl,
    String xmlUrl,
    Instant issueDate
) {}
