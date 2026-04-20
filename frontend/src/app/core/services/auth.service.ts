import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError, EMPTY } from 'rxjs';
import { environment } from '@env/environment';
import { AuthRequest, AuthResponse, AuthState, UserRole } from '@core/models/auth.model';

/**
 * Gerencia o estado de autenticação via Signals (Angular 17).
 *
 * O token JWT fica em cookie HTTP-Only — este serviço nunca o lê nem armazena.
 * Mantém apenas metadados: role e expiração para decisões de UI/routing.
 * Em caso de reload, o estado é restaurado via endpoint /auth/refresh.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private readonly _state = signal<AuthState>({
    role: null,
    email: null,
    expiresAt: null,
    isAuthenticated: false,
  });

  readonly isAuthenticated = computed(() => this._state().isAuthenticated);
  readonly role = computed(() => this._state().role);
  readonly email = computed(() => this._state().email);
  readonly isAdmin = computed(() =>
    this._state().role === 'ROLE_ADMIN'
  );
  readonly isPrivileged = computed(() =>
    this._state().role === 'ROLE_ADMIN' || this._state().role === 'ROLE_GERENTE'
  );

  constructor(private http: HttpClient, private router: Router) {
    this.tryRestoreSession();
  }

  login(request: AuthRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.applyAuthState(response))
    );
  }

  logout() {
    this.http.post<void>(`${this.apiUrl}/logout`, {}).pipe(
      catchError(() => EMPTY)
    ).subscribe(() => {
      this._state.set({ role: null, email: null, expiresAt: null, isAuthenticated: false });
      this.router.navigate(['/login']);
    });
  }

  refreshToken() {
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}).pipe(
      tap(response => this.applyAuthState(response))
    );
  }

  private applyAuthState(response: AuthResponse) {
    this._state.set({
      role: response.role as UserRole,
      email: response.email ?? null,
      expiresAt: Date.now() + response.expiresIn * 1000,
      isAuthenticated: true,
    });
  }

  private tryRestoreSession() {
    // Tenta renovar o access token usando o refresh token do cookie HTTP-Only.
    // Se o cookie não existir ou estiver expirado, o backend retorna 401
    // e o interceptor de erro irá redirecionar para /login.
    this.refreshToken().pipe(
      catchError(() => {
        this._state.set({ role: null, email: null, expiresAt: null, isAuthenticated: false });
        return EMPTY;
      })
    ).subscribe();
  }
}
