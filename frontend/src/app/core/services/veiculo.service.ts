import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '@env/environment';
import { PageResponse, Veiculo, VeiculoRequest } from '@core/models/veiculo.model';

@Injectable({ providedIn: 'root' })
export class VeiculoService {
  private readonly base = `${environment.apiUrl}/veiculos`;

  constructor(private http: HttpClient) {}

  listar(page = 0, size = 20, q?: string) {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    if (q) params = params.set('q', q);
    return this.http.get<PageResponse<Veiculo>>(this.base, { params });
  }

  buscarPorId(id: string) {
    return this.http.get<Veiculo>(`${this.base}/${id}`);
  }

  criar(request: VeiculoRequest) {
    return this.http.post<Veiculo>(this.base, request);
  }

  deletar(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
