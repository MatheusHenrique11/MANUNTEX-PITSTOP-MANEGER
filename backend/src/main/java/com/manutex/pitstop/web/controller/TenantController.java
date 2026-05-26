package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.service.CnpjLookupService;
import com.manutex.pitstop.service.TenantRegistrationService;
import com.manutex.pitstop.web.dto.CnpjLookupResponse;
import com.manutex.pitstop.web.dto.EmpresaResponse;
import com.manutex.pitstop.web.dto.TenantRegistrationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRegistrationService tenantRegistrationService;
    private final CnpjLookupService cnpjLookupService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse register(@Valid @RequestBody TenantRegistrationRequest request) {
        return tenantRegistrationService.registrar(request);
    }

    @GetMapping("/lookup-cnpj/{cnpj}")
    public CnpjLookupResponse lookupCnpj(@PathVariable String cnpj) {
        return cnpjLookupService.lookup(cnpj);
    }
}
