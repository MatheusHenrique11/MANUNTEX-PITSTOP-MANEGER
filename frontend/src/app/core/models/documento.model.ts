export type TipoDocumento =
  | 'CRLV'
  | 'CNH'
  | 'SEGURO'
  | 'LAUDO_CAUTELAR'
  | 'NOTA_FISCAL'
  | 'OUTRO';

export interface Documento {
  id: string;
  veiculoId?: string;
  clienteId?: string;
  tipo: TipoDocumento;
  nomeOriginal: string;
  tamanhoBytes: number;
  checksumSha256: string;
  createdAt: string;
  expiresAt?: string;
  viewUrl?: string;     // preenchido apenas em GET /documentos/{id}/view
}
