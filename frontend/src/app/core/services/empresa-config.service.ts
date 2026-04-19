import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '@env/environment';
import { EmpresaConfigRequest, EmpresaConfigResponse } from '@core/models/empresa-config.model';

@Injectable({ providedIn: 'root' })
export class EmpresaConfigService {
  private readonly base = `${environment.apiUrl}/empresa`;

  constructor(private http: HttpClient) {}

  buscar() {
    return this.http.get<EmpresaConfigResponse>(this.base);
  }

  salvar(request: EmpresaConfigRequest) {
    return this.http.put<EmpresaConfigResponse>(this.base, request);
  }

  uploadLogo(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<EmpresaConfigResponse>(`${this.base}/logo`, formData);
  }
}
