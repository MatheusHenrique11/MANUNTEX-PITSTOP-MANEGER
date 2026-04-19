import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { signal } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

describe('authGuard', () => {
  let router: Router;
  let mockAuthService: Partial<AuthService>;

  beforeEach(() => {
    mockAuthService = {
      isAuthenticated: signal(false) as any,
    };

    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [{ provide: AuthService, useValue: mockAuthService }],
    });

    router = TestBed.inject(Router);
  });

  function runGuard() {
    return TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
  }

  it('deve permitir acesso quando autenticado', () => {
    (mockAuthService.isAuthenticated as any) = signal(true);
    const result = runGuard();
    expect(result).toBeTrue();
  });

  it('deve redirecionar para /login quando não autenticado', () => {
    (mockAuthService.isAuthenticated as any) = signal(false);
    const result = runGuard();
    // Retorna UrlTree (redirect) quando não autenticado
    expect(result).not.toBeTrue();
    expect((result as any).toString()).toContain('/login');
  });
});
