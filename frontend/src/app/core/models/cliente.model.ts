export interface Cliente {
  id: string;
  nome: string;
  cpfCnpj: string;
  telefone?: string;
  email?: string;
}

export interface ClienteRequest {
  nome: string;
  cpfCnpj: string;
  telefone?: string;
  email?: string;
}
