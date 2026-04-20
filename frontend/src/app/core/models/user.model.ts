export type UserRole = 'ROLE_ADMIN' | 'ROLE_GERENTE' | 'ROLE_MECANICO' | 'ROLE_RECEPCIONISTA';

export const ROLE_LABELS: Record<UserRole, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_GERENTE: 'Gerente',
  ROLE_MECANICO: 'Mecânico',
  ROLE_RECEPCIONISTA: 'Recepcionista',
};

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
}

export interface UserRequest {
  email: string;
  password: string;
  fullName: string;
  role: UserRole;
}
