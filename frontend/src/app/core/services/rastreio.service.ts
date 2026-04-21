import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '@env/environment';
import { RastreioResponse } from '@core/models/rastreio.model';

@Injectable({ providedIn: 'root' })
export class RastreioService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/public/rastreio`;

  buscar(token: string) {
    return this.http.get<RastreioResponse>(`${this.url}/${token}`);
  }
}
