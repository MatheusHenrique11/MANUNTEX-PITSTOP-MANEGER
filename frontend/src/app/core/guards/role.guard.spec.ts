import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let mockAuthService: Partial<AuthService>;

  function buildRoute(roles: string[]): ActivatedRouteSnapshot {
    const route = new ActivatedRouteSnapshot();
    (route as any).data = { roles };
    return route;
  }

  function runGuard(roles: string[]) {
    return TestBed.runInInjectionContext(() =>
      roleGuard(buildRoute(roles), {} as RouterStateSnapshot)
    );
  }

  beforeEach(() => {
    mockAuthService = { role: signal(null) as any };

    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [{ provide: AuthService, useValue: mockAuthService }],
    });
  });

  it('deve permitir se o usuário tem o role exigido', () => {
    (mockAuthService.role as any) = signal('ROLE_ADMIN');
    expect(runGuard(['ROLE_ADMIN'])).toBeTrue();
  });

  it('deve bloquear se o usuário não tem o role exigido', () => {
    (mockAuthService.role as any) = signal('ROLE_MECANICO');
    const result = runGuard(['ROLE_ADMIN']);
    expect(result).not.toBeTrue();
    expect((result as any).toString()).toContain('/403');
  });

  it('deve permitir se a rota não exige roles específicos', () => {
    (mockAuthService.role as any) = signal('ROLE_MECANICO');
    expect(runGuard([])).toBeTrue();
  });

  it('deve aceitar qualquer role da lista', () => {
    (mockAuthService.role as any) = signal('ROLE_GERENTE');
    expect(runGuard(['ROLE_ADMIN', 'ROLE_GERENTE'])).toBeTrue();
  });
});
