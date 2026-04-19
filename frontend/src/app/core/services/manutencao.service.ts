import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '@env/environment';
import {
  ManutencaoRequest,
  ManutencaoResponse,
  ManutencaoUpdateRequest,
  ManutencaoPrintResponse,
  StatusUpdateRequest,
  StatusManutencao,
} from '@core/models/manutencao.model';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class ManutencaoService {
  private readonly base = `${environment.apiUrl}/manutencoes`;

  constructor(private http: HttpClient) {}

  criar(request: ManutencaoRequest) {
    return this.http.post<ManutencaoResponse>(this.base, request);
  }

  listar(page = 0, size = 20, status?: StatusManutencao) {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'dataEntrada,desc');
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<ManutencaoResponse>>(this.base, { params });
  }

  buscarPorId(id: string) {
    return this.http.get<ManutencaoResponse>(`${this.base}/${id}`);
  }

  listarPorVeiculo(veiculoId: string, page = 0, size = 20, status?: StatusManutencao) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<ManutencaoResponse>>(`${this.base}/veiculo/${veiculoId}`, { params });
  }

  atualizar(id: string, request: ManutencaoUpdateRequest) {
    return this.http.put<ManutencaoResponse>(`${this.base}/${id}`, request);
  }

  alterarStatus(id: string, status: StatusManutencao) {
    const body: StatusUpdateRequest = { status };
    return this.http.patch<ManutencaoResponse>(`${this.base}/${id}/status`, body);
  }

  buscarParaImpressao(id: string) {
    return this.http.get<ManutencaoPrintResponse>(`${this.base}/${id}/imprimir`);
  }

  deletar(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
