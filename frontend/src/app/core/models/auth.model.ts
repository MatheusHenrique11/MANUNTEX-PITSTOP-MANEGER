export interface AuthRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  expiresIn: number;
  role: string;
  email?: string;
}

export type UserRole =
  | 'ROLE_ADMIN'
  | 'ROLE_GERENTE'
  | 'ROLE_MECANICO'
  | 'ROLE_RECEPCIONISTA';

export interface AuthState {
  role: UserRole | null;
  email: string | null;
  expiresAt: number | null;
  isAuthenticated: boolean;
}
