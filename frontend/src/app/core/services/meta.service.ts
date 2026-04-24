import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '@env/environment';
import { MetaRequest, MetaResponse, ProducaoMecanicoResponse } from '@core/models/meta.model';

@Injectable({ providedIn: 'root' })
export class MetaService {
  private readonly base = `${environment.apiUrl}/metas`;

  constructor(private http: HttpClient) {}

  definirMeta(request: MetaRequest) {
    return this.http.post<MetaResponse>(this.base, request);
  }

  listarProducaoGeral(mes: number, ano: number) {
    const params = new HttpParams().set('mes', mes).set('ano', ano);
    return this.http.get<ProducaoMecanicoResponse[]>(this.base, { params });
  }

  buscarProducaoMecanico(mecanicoId: string, mes: number, ano: number) {
    const params = new HttpParams().set('mes', mes).set('ano', ano);
    return this.http.get<ProducaoMecanicoResponse>(
      `${this.base}/mecanico/${mecanicoId}`, { params }
    );
  }

  buscarMinhaProducao(mes: number, ano: number) {
    const params = new HttpParams().set('mes', mes).set('ano', ano);
    return this.http.get<ProducaoMecanicoResponse>(`${this.base}/minhas`, { params });
  }

  buscarMeta(mecanicoId: string, mes: number, ano: number) {
    const params = new HttpParams().set('mes', mes).set('ano', ano);
    return this.http.get<MetaResponse>(`${this.base}/mecanico/${mecanicoId}/meta`, { params });
  }

  baixarPdfRelatorio(mes: number, ano: number) {
    const params = new HttpParams().set('mes', mes).set('ano', ano);
    return this.http.get(`${this.base}/relatorio/pdf`, {
      params,
      responseType: 'blob',
    });
  }
}
