import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type FiscalEnvironment = 'HOMOLOGACAO' | 'PRODUCAO';

export interface PlatformFiscalConfigResponse {
  id: string;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpjMascarado?: string;
  inscricaoMunicipal?: string;
  regimeTributario?: string;
  codigoMunicipio: string;
  municipio?: string;
  uf?: string;
  endereco?: string;
  numero?: string;
  bairro?: string;
  cep?: string;
  emailFiscal?: string;
  telefoneFiscal?: string;
  codigoServicoMunicipal?: string;
  itemListaServico?: string;
  aliquotaIss?: number;
  ambienteFiscal: FiscalEnvironment;
  readyForProduction: boolean;
  updatedAt?: string;
  updatedBy?: string;
}

export interface PlatformFiscalConfigRequest {
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  inscricaoMunicipal?: string;
  regimeTributario?: string;
  codigoMunicipio: string;
  municipio?: string;
  uf?: string;
  endereco?: string;
  numero?: string;
  bairro?: string;
  cep?: string;
  emailFiscal?: string;
  telefoneFiscal?: string;
  codigoServicoMunicipal?: string;
  itemListaServico?: string;
  aliquotaIss?: number;
  ambienteFiscal: FiscalEnvironment;
}

export interface TenantFiscalConfigResponse {
  id: string;
  empresaId: string;
  razaoSocial?: string;
  nomeFantasia?: string;
  cnpjMascarado?: string;
  inscricaoMunicipal?: string;
  regimeTributario?: string;
  codigoMunicipio?: string;
  municipio?: string;
  uf?: string;
  endereco?: string;
  numero?: string;
  bairro?: string;
  cep?: string;
  emailFiscal?: string;
  telefoneFiscal?: string;
  codigoServicoMunicipal?: string;
  itemListaServico?: string;
  aliquotaIss?: number;
  ambienteFiscal: FiscalEnvironment;
  fiscalEnabled: boolean;
  readyForEmission: boolean;
  fiscalValidatedAt?: string;
  updatedAt?: string;
}

export interface TenantFiscalConfigRequest {
  razaoSocial?: string;
  nomeFantasia?: string;
  cnpj?: string;
  inscricaoMunicipal?: string;
  regimeTributario?: string;
  codigoMunicipio?: string;
  municipio?: string;
  uf?: string;
  endereco?: string;
  numero?: string;
  bairro?: string;
  cep?: string;
  emailFiscal?: string;
  telefoneFiscal?: string;
  codigoServicoMunicipal?: string;
  itemListaServico?: string;
  aliquotaIss?: number;
  ambienteFiscal?: FiscalEnvironment;
  fiscalEnabled: boolean;
}

@Injectable({ providedIn: 'root' })
export class FiscalService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getPlatformFiscalConfig(): Observable<PlatformFiscalConfigResponse> {
    return this.http.get<PlatformFiscalConfigResponse>(
      `${this.base}/admin/fiscal/platform`
    );
  }

  savePlatformFiscalConfig(
    request: PlatformFiscalConfigRequest
  ): Observable<PlatformFiscalConfigResponse> {
    return this.http.put<PlatformFiscalConfigResponse>(
      `${this.base}/admin/fiscal/platform`,
      request
    );
  }

  getTenantFiscalConfig(): Observable<TenantFiscalConfigResponse> {
    return this.http.get<TenantFiscalConfigResponse>(
      `${this.base}/fiscal/tenant`
    );
  }

  saveTenantFiscalConfig(
    request: TenantFiscalConfigRequest
  ): Observable<TenantFiscalConfigResponse> {
    return this.http.put<TenantFiscalConfigResponse>(
      `${this.base}/fiscal/tenant`,
      request
    );
  }
}
