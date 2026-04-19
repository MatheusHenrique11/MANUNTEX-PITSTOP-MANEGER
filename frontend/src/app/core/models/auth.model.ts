export interface AuthRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  expiresIn: number;
  role: string;
}

export type UserRole =
  | 'ROLE_ADMIN'
  | 'ROLE_GERENTE'
  | 'ROLE_MECANICO'
  | 'ROLE_RECEPCIONISTA';

export interface AuthState {
  role: UserRole | null;
  expiresAt: number | null;   // timestamp em ms
  isAuthenticated: boolean;
}
