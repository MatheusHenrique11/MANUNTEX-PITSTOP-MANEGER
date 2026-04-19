import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '@env/environment';
import { Documento, TipoDocumento } from '@core/models/documento.model';

@Injectable({ providedIn: 'root' })
export class DocumentoService {
  private readonly base = `${environment.apiUrl}/documentos`;

  constructor(private http: HttpClient) {}

  listarPorVeiculo(veiculoId: string) {
    return this.http.get<Documento[]>(`${this.base}/veiculo/${veiculoId}`);
  }

  /**
   * Solicita o conteúdo do PDF descriptografado como Blob.
   * O backend realiza a descriptografia e serve os bytes diretamente.
   * O PDF nunca fica público no S3.
   */
  getContent(documentoId: string) {
    return this.http.get(`${this.base}/${documentoId}/view`, {
      responseType: 'blob',
    });
  }

  upload(file: File, tipo: TipoDocumento, veiculoId?: string, clienteId?: string) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('tipo', tipo);
    if (veiculoId) formData.append('veiculoId', veiculoId);
    if (clienteId) formData.append('clienteId', clienteId);
    return this.http.post<Documento>(this.base, formData);
  }

  deletar(id: string) {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  /** Cria uma URL de Object URL temporária do Blob para exibir no browser */
  createBlobUrl(blob: Blob): string {
    return URL.createObjectURL(blob);
  }

  revokeBlobUrl(url: string): void {
    URL.revokeObjectURL(url);
  }
}
