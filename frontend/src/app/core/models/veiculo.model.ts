export interface Veiculo {
  id: string;
  placa: string;
  chassi: string;      // pode vir mascarado: "************1234"
  renavam: string;     // pode vir mascarado: "********590"
  marca: string;
  modelo: string;
  anoFabricacao: number;
  anoModelo: number;
  cor?: string;
  clienteId: string;
  createdAt: string;
}

export interface VeiculoRequest {
  placa: string;
  chassi: string;
  renavam: string;
  marca: string;
  modelo: string;
  anoFabricacao: number;
  anoModelo: number;
  cor?: string;
  clienteId: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
