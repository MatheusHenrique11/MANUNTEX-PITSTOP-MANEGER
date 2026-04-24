import { StatusManutencao } from './manutencao.model';

export interface MetaRequest {
  mecanicoId: string;
  mes: number;
  ano: number;
  valorMeta: number;
}

export interface MetaResponse {
  id: string;
  mecanicoId: string;
  mecanicoNome: string;
  mes: number;
  ano: number;
  valorMeta: number;
  createdAt: string;
  updatedAt: string;
}

export interface ServicoMecanicoItem {
  manutencaoId: string;
  veiculoPlaca: string;
  veiculoMarca: string;
  veiculoModelo: string;
  clienteNome: string | null;
  descricao: string;
  valorFinal: number | null;
  dataConclusao: string | null;
  status: StatusManutencao;
}

export interface ProducaoMecanicoResponse {
  mecanicoId: string;
  mecanicoNome: string;
  mes: number;
  ano: number;
  totalServicos: number;
  totalValorProduzido: number;
  valorMeta: number | null;
  percentualAtingido: number;
  metaBatida: boolean;
  servicos: ServicoMecanicoItem[];
}
