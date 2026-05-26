import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface TenantRegistrationRequest {
  cnpj: string;
  razaoSocial: string;
  nomeFantasia?: string;
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: string;
  codigoMunicipioIbge?: string;
  emailFiscal?: string;
  telefoneFiscal?: string;
  gerenteNome: string;
  gerenteEmail: string;
  gerenteSenha: string;
}

export interface CnpjLookupResponse {
  cnpj: string;
  razaoSocial: string;
  nomeFantasia?: string;
  cep?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  uf?: string;
  codigoMunicipioIbge?: string;
  emailFiscal?: string;
  telefoneFiscal?: string;
  situacaoCadastral?: string;
}

export interface TenantRegistrationResponse {
  id: string;
  nome: string;
  cnpj: string;
}

@Injectable({ providedIn: 'root' })
export class TenantService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/tenants`;

  lookupCnpj(cnpj: string): Observable<CnpjLookupResponse> {
    const digits = cnpj.replace(/\D/g, '');
    return this.http.get<CnpjLookupResponse>(`${this.apiUrl}/lookup-cnpj/${digits}`);
  }

  register(request: TenantRegistrationRequest): Observable<TenantRegistrationResponse> {
    return this.http.post<TenantRegistrationResponse>(`${this.apiUrl}/register`, request);
  }
}
