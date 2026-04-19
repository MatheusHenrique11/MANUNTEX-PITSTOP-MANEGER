export type StatusManutencao =
  | 'ABERTA'
  | 'EM_ANDAMENTO'
  | 'AGUARDANDO_PECAS'
  | 'CONCLUIDA'
  | 'CANCELADA';

export interface ManutencaoResponse {
  id: string;
  veiculoId: string;
  veiculoPlaca: string;
  veiculoMarca: string;
  veiculoModelo: string;
  veiculoCor: string | null;
  clienteNome: string | null;
  clienteTelefone: string | null;
  mecanicoId: string;
  mecanicoNome: string;
  descricao: string;
  kmEntrada: number | null;
  kmSaida: number | null;
  relatorio: string | null;
  observacoes: string | null;
  orcamento: number | null;
  valorFinal: number | null;
  status: StatusManutencao;
  dataEntrada: string;
  dataConclusao: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ManutencaoRequest {
  veiculoId: string;
  mecanicoId: string;
  descricao: string;
  kmEntrada?: number;
  relatorio?: string;
  observacoes?: string;
  orcamento?: number;
  valorFinal?: number;
  kmSaida?: number;
}

export interface ManutencaoUpdateRequest {
  mecanicoId?: string;
  descricao?: string;
  kmEntrada?: number;
  kmSaida?: number;
  relatorio?: string;
  observacoes?: string;
  orcamento?: number;
  valorFinal?: number;
}

export interface StatusUpdateRequest {
  status: StatusManutencao;
}

export interface ManutencaoPrintResponse {
  ordemDeServico: ManutencaoResponse;
  empresa: EmpresaConfigResponse;
}

export interface EmpresaConfigResponse {
  id: string;
  nome: string;
  cnpj: string | null;
  endereco: string | null;
  telefone: string | null;
  email: string | null;
  logoUrl: string | null;
}
