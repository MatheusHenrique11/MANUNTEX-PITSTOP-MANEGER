import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { featureFlagGuard } from './feature-flag.guard';
import { FeatureFlagService } from '../services/feature-flag.service';
import { AuthService } from '../services/auth.service';
import { DISABLED_MODULE_PARAM } from '../models/feature-flag.model';

describe('featureFlagGuard', () => {
  let mockFeatureFlagService: Partial<FeatureFlagService>;
  let mockIsAdmin: ReturnType<typeof signal<boolean>>;

  function buildRoute(feature: string): ActivatedRouteSnapshot {
    const route = new ActivatedRouteSnapshot();
    (route as any).data = { feature };
    return route;
  }

  function runGuard(feature: string) {
    return TestBed.runInInjectionContext(() =>
      featureFlagGuard(buildRoute(feature), {} as RouterStateSnapshot)
    );
  }

  beforeEach(() => {
    mockIsAdmin = signal(false);
    mockFeatureFlagService = {
      isActive: jasmine.createSpy('isActive').and.returnValue(false),
    };

    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        { provide: FeatureFlagService, useValue: mockFeatureFlagService },
        { provide: AuthService, useValue: { isAdmin: mockIsAdmin } },
      ],
    });
  });

  it('deve permitir acesso quando módulo está ativo', () => {
    (mockFeatureFlagService.isActive as jasmine.Spy).and.returnValue(true);
    expect(runGuard('VEHICLE_MANAGEMENT')).toBeTrue();
  });

  it('deve redirecionar quando módulo está desativado', () => {
    const result = runGuard('MAINTENANCE_MODULE');
    expect(result).not.toBeTrue();
  });

  it('deve redirecionar para /dashboard (não para raiz) com módulo desativado', () => {
    const result = runGuard('VEHICLE_MANAGEMENT') as any;
    expect(result.toString()).toContain('/dashboard');
    expect(result.toString()).not.toBe('/');
  });

  it('deve usar DISABLED_MODULE_PARAM como chave do queryParam no redirect', () => {
    const result = runGuard('VEHICLE_MANAGEMENT') as any;
    expect(result.queryParams?.[DISABLED_MODULE_PARAM]).toBe('VEHICLE_MANAGEMENT');
  });

  it('deve incluir o nome do módulo no queryParam do redirect', () => {
    const result = runGuard('MAINTENANCE_MODULE') as any;
    expect(result.queryParams?.[DISABLED_MODULE_PARAM]).toBe('MAINTENANCE_MODULE');
  });

  it('deve permitir acesso para ROLE_ADMIN mesmo com módulo desativado', () => {
    mockIsAdmin.set(true);
    (mockFeatureFlagService.isActive as jasmine.Spy).and.returnValue(false);
    expect(runGuard('VEHICLE_MANAGEMENT')).toBeTrue();
  });

  it('deve bloquear acesso para não-admin com módulo desativado', () => {
    mockIsAdmin.set(false);
    (mockFeatureFlagService.isActive as jasmine.Spy).and.returnValue(false);
    const result = runGuard('VEHICLE_MANAGEMENT');
    expect(result).not.toBeTrue();
  });

  it('deve chamar isActive com o nome correto da feature', () => {
    runGuard('DOCUMENT_VAULT');
    expect(mockFeatureFlagService.isActive).toHaveBeenCalledWith('DOCUMENT_VAULT');
  });

  it('deve não chamar isActive quando usuário é admin', () => {
    mockIsAdmin.set(true);
    runGuard('DOCUMENT_VAULT');
    expect(mockFeatureFlagService.isActive).not.toHaveBeenCalled();
  });

  it('deve permitir quando não há feature configurada na rota', () => {
    const route = new ActivatedRouteSnapshot();
    (route as any).data = {};
    const result = TestBed.runInInjectionContext(() =>
      featureFlagGuard(route, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
  });
});
