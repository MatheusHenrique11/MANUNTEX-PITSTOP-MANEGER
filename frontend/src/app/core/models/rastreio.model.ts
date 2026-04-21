import { StatusManutencao } from './manutencao.model';

export interface RastreioResponse {
  trackingToken: string;
  status: StatusManutencao;
  veiculoPlaca: string;
  veiculoMarca: string;
  veiculoModelo: string;
  veiculoCor: string | null;
  mecanicoNome: string;
  descricao: string;
  observacoes: string | null;
  dataEntrada: string;
  dataConclusao: string | null;
}

export const STATUS_CONFIG: Record<StatusManutencao, { label: string; color: string; icon: string; step: number }> = {
  ABERTA:           { label: 'Aberta',            color: 'text-slate-400',    icon: '○', step: 1 },
  EM_ANDAMENTO:     { label: 'Em Andamento',       color: 'text-petroleum-400', icon: '◉', step: 2 },
  AGUARDANDO_PECAS: { label: 'Aguardando Peças',   color: 'text-safety-400',   icon: '◷', step: 3 },
  CONCLUIDA:        { label: 'Concluída',           color: 'text-success-400',  icon: '✓', step: 4 },
  CANCELADA:        { label: 'Cancelada',           color: 'text-danger-400',   icon: '✕', step: 0 },
};
