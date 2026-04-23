import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '@env/environment';
import { Cliente, ClienteRequest } from '@core/models/cliente.model';
import { PageResponse } from '@core/models/veiculo.model';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly base = `${environment.apiUrl}/clientes`;

  constructor(private http: HttpClient) {}

  listar(q?: string, page = 0, size = 20) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    return this.http.get<PageResponse<Cliente>>(this.base, { params });
  }

  buscarPorId(id: string) {
    return this.http.get<Cliente>(`${this.base}/${id}`);
  }

  criar(request: ClienteRequest) {
    return this.http.post<Cliente>(this.base, request);
  }
}
