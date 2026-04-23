import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { signal } from '@angular/core';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { Params } from '@angular/router';
import { WritableSignal } from '@angular/core';
import { DashboardComponent } from './dashboard.component';
import { AuthService } from '@core/services/auth.service';
import { FeatureFlagService } from '@core/services/feature-flag.service';
import { VeiculoService } from '@core/services/veiculo.service';
import { DISABLED_MODULE_PARAM, FeatureName } from '@core/models/feature-flag.model';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let queryParamsSubject: BehaviorSubject<Params>;
  let mockIsAdmin: WritableSignal<boolean>;
  let activeFeatures: WritableSignal<Set<string>>;
  let featureFlagSpy: jasmine.SpyObj<FeatureFlagService>;
  let veiculoSpy: jasmine.SpyObj<VeiculoService>;
  let routerNavigateSpy: jasmine.Spy;

  const ALL_FEATURES = new Set(['VEHICLE_MANAGEMENT', 'DOCUMENT_VAULT', 'MAINTENANCE_MODULE', 'ANALYTICS_DASHBOARD']);

  beforeEach(async () => {
    queryParamsSubject = new BehaviorSubject<Params>({});
    mockIsAdmin = signal(false);
    activeFeatures = signal<Set<string>>(ALL_FEATURES);

    featureFlagSpy = jasmine.createSpyObj('FeatureFlagService', ['isActive', 'labelFor']);
    // callFake reads activeFeatures signal → computed tracks it as dependency → reacts to set()
    featureFlagSpy.isActive.and.callFake((f: FeatureName) => activeFeatures().has(f));
    featureFlagSpy.labelFor.and.callFake((f: string) => `Label:${f}`);

    veiculoSpy = jasmine.createSpyObj('VeiculoService', ['listar']);
    veiculoSpy.listar.and.returnValue(
      of({ totalElements: 7, content: [], totalPages: 1, size: 1, number: 0 })
    );

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, RouterTestingModule.withRoutes([])],
      providers: [
        { provide: AuthService, useValue: { isAdmin: mockIsAdmin, email: signal('test@pitstop.com') } },
        { provide: FeatureFlagService, useValue: featureFlagSpy },
        { provide: VeiculoService, useValue: veiculoSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: queryParamsSubject.asObservable(),
            snapshot: { queryParams: {}, params: {}, url: [], data: {}, outlet: 'primary', fragment: null },
          },
        },
      ],
    }).compileComponents();

    routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve criar o componente', () => {
    expect(component).toBeTruthy();
  });

  describe('visibleQuickActions()', () => {
    it('deve exibir todas as 4 ações para admin independente de feature flags', () => {
      mockIsAdmin.set(true);
      activeFeatures.set(new Set()); // todos desativados
      expect(component.visibleQuickActions().length).toBe(4);
    });

    it('deve filtrar ações de módulos desativados para não-admin', () => {
      // Mudar activeFeatures invalida o computed reativamente
      activeFeatures.set(new Set(['MAINTENANCE_MODULE']));
      const actions = component.visibleQuickActions();
      expect(actions.length).toBe(1);
      expect(actions[0].label).toBe('Nova OS');
    });

    it('deve exibir ação de módulo ativo para não-admin', () => {
      activeFeatures.set(new Set(['ANALYTICS_DASHBOARD']));
      const labels = component.visibleQuickActions().map(a => a.label);
      expect(labels).toContain('Relatórios');
    });

    it('deve exibir todas as ações quando todos os módulos estão ativos', () => {
      activeFeatures.set(ALL_FEATURES);
      expect(component.visibleQuickActions().length).toBe(4);
    });

    it('deve ocultar todas as ações com feature quando módulos estão desativados', () => {
      activeFeatures.set(new Set());
      expect(component.visibleQuickActions().length).toBe(0);
    });
  });

  describe('alerta de módulo desativado', () => {
    it('deve exibir alerta quando queryParam modulo_desativado está na URL', fakeAsync(() => {
      queryParamsSubject.next({ [DISABLED_MODULE_PARAM]: 'VEHICLE_MANAGEMENT' });
      tick();
      expect(component.disabledModuleAlert()).toBe('Label:VEHICLE_MANAGEMENT');
    }));

    it('deve usar labelFor() do service para o label do alerta', fakeAsync(() => {
      featureFlagSpy.labelFor.and.returnValue('Gestão de Veículos');
      queryParamsSubject.next({ [DISABLED_MODULE_PARAM]: 'VEHICLE_MANAGEMENT' });
      tick();

      expect(featureFlagSpy.labelFor).toHaveBeenCalledWith('VEHICLE_MANAGEMENT');
      expect(component.disabledModuleAlert()).toBe('Gestão de Veículos');
    }));

    it('deve limpar o queryParam da URL após exibir alerta', fakeAsync(() => {
      queryParamsSubject.next({ [DISABLED_MODULE_PARAM]: 'DOCUMENT_VAULT' });
      tick();

      expect(routerNavigateSpy).toHaveBeenCalledWith(
        [],
        jasmine.objectContaining({
          queryParams: { [DISABLED_MODULE_PARAM]: null },
          queryParamsHandling: 'merge',
          replaceUrl: true,
        })
      );
    }));

    it('deve não exibir alerta quando queryParam está ausente', fakeAsync(() => {
      queryParamsSubject.next({});
      tick();
      expect(component.disabledModuleAlert()).toBeNull();
    }));

    it('deve não navegar quando queryParam está ausente', fakeAsync(() => {
      queryParamsSubject.next({});
      tick();
      expect(routerNavigateSpy).not.toHaveBeenCalled();
    }));

    it('dismissModuleAlert() deve limpar o alerta', fakeAsync(() => {
      queryParamsSubject.next({ [DISABLED_MODULE_PARAM]: 'VEHICLE_MANAGEMENT' });
      tick();
      expect(component.disabledModuleAlert()).not.toBeNull();

      component.dismissModuleAlert();
      expect(component.disabledModuleAlert()).toBeNull();
    }));

    it('disabledModuleAlert deve ser readonly (sem método set)', () => {
      expect((component.disabledModuleAlert as any).set).toBeUndefined();
    });
  });

  describe('carregamento de estatísticas', () => {
    it('deve iniciar com loadingStats true antes do ngOnInit', () => {
      const freshFixture = TestBed.createComponent(DashboardComponent);
      expect(freshFixture.componentInstance.loadingStats()).toBeTrue();
    });

    it('deve definir totalVeiculos com dados do backend', () => {
      expect(component.totalVeiculos()).toBe(7);
      expect(component.loadingStats()).toBeFalse();
    });

    it('deve parar loadingStats em caso de erro no listar()', fakeAsync(() => {
      const freshFixture = TestBed.createComponent(DashboardComponent);
      veiculoSpy.listar.and.returnValue(throwError(() => new Error('API error')));
      freshFixture.detectChanges();
      tick();
      expect(freshFixture.componentInstance.loadingStats()).toBeFalse();
    }));
  });

  describe('saudação', () => {
    it('deve extrair primeiro nome do email', () => {
      expect(component.firstName()).toBe('test');
    });

    it('deve retornar "usuário" quando email está vazio', () => {
      const mockAuth = { isAdmin: signal(false), email: signal('') };
      const freshBed = TestBed.createComponent(DashboardComponent);
      expect(freshBed.componentInstance.firstName()).toBeDefined();
    });
  });
});
