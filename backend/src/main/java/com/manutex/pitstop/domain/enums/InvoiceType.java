package com.manutex.pitstop.domain.enums;

public enum InvoiceType {
    /** NFS-e da assinatura SaaS: prestador = RiseCode Studio, tomador = oficina. */
    SAAS,
    /** NFS-e de serviço automotivo: prestador = oficina, tomador = cliente final. */
    WORKSHOP
}
