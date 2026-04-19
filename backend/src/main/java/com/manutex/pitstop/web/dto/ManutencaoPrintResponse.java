package com.manutex.pitstop.web.dto;

public record ManutencaoPrintResponse(
    ManutencaoResponse ordemDeServico,
    EmpresaConfigResponse empresa
) {}
