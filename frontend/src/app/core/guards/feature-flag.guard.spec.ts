import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { featureFlagGuard } from './feature-flag.guard';
import { FeatureFlagService } from '../services/feature-flag.service';

describe('featureFlagGuard', () => {
  let mockFeatureFlagService: Partial<FeatureFlagService>;

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
    mockFeatureFlagService = {
      isActive: jasmine.createSpy('isActive').and.returnValue(false),
    };

    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [{ provide: FeatureFlagService, useValue: mockFeatureFlagService }],
    });
  });

  it('deve permitir acesso quando módulo está ativo', () => {
    (mockFeatureFlagService.isActive as jasmine.Spy).and.returnValue(true);
    expect(runGuard('VEHICLE_MANAGEMENT')).toBeTrue();
  });

  it('deve redirecionar quando módulo está desativado', () => {
    (mockFeatureFlagService.isActive as jasmine.Spy).and.returnValue(false);
    const result = runGuard('MAINTENANCE_MODULE');
    expect(result).not.toBeTrue();
    expect((result as any).toString()).toContain('/');
  });

  it('deve incluir o nome do módulo no queryParam do redirect', () => {
    (mockFeatureFlagService.isActive as jasmine.Spy).and.returnValue(false);
    const result = runGuard('MAINTENANCE_MODULE') as any;
    expect(result.queryParams?.modulo_desativado).toBe('MAINTENANCE_MODULE');
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
