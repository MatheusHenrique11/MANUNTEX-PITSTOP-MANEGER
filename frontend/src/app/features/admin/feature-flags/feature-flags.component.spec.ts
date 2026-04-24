import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { FeatureFlagsComponent } from './feature-flags.component';
import { FeatureFlagService } from '../../../core/services/feature-flag.service';
import { FeatureFlagsMap } from '../../../core/models/feature-flag.model';

const mockFlags: FeatureFlagsMap = {
  VEHICLE_MANAGEMENT:  { active: true,  label: 'Gestão de Veículos' },
  DOCUMENT_VAULT:      { active: true,  label: 'Cofre de Documentos' },
  MAINTENANCE_MODULE:  { active: false, label: 'Manutenções' },
  ANALYTICS_DASHBOARD: { active: false, label: 'Dashboard Analítico' },
  NOTIFICATIONS:       { active: false, label: 'Notificações' },
  FINANCIAL_MODULE:    { active: false, label: 'Financeiro' },
  DETRAN_INTEGRATION:  { active: false, label: 'Integração DETRAN' },
  GOALS_MODULE:        { active: false, label: 'Metas por Mecânico' },
};

describe('FeatureFlagsComponent', () => {
  let featureFlagServiceSpy: jasmine.SpyObj<FeatureFlagService>;

  beforeEach(async () => {
    featureFlagServiceSpy = jasmine.createSpyObj('FeatureFlagService', ['load']);
    featureFlagServiceSpy.load.and.returnValue(of(mockFlags));

    await TestBed.configureTestingModule({
      imports: [FeatureFlagsComponent],
      providers: [{ provide: FeatureFlagService, useValue: featureFlagServiceSpy }],
    }).compileComponents();
  });

  it('deve criar o componente', () => {
    const fixture = TestBed.createComponent(FeatureFlagsComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('deve carregar e exibir todas as flags', fakeAsync(() => {
    const fixture = TestBed.createComponent(FeatureFlagsComponent);
    fixture.detectChanges();
    tick();

    const comp = fixture.componentInstance;
    expect(comp.flags().length).toBe(7);
    expect(comp.loading()).toBeFalse();
  }));

  it('deve mapear status corretamente', fakeAsync(() => {
    const fixture = TestBed.createComponent(FeatureFlagsComponent);
    fixture.detectChanges();
    tick();

    const flags = fixture.componentInstance.flags();
    const vehicleFlag = flags.find(f => f.name === 'VEHICLE_MANAGEMENT')!;
    const maintenanceFlag = flags.find(f => f.name === 'MAINTENANCE_MODULE')!;

    expect(vehicleFlag.active).toBeTrue();
    expect(vehicleFlag.label).toBe('Gestão de Veículos');
    expect(maintenanceFlag.active).toBeFalse();
  }));

  it('deve parar o loading em caso de erro', fakeAsync(() => {
    featureFlagServiceSpy.load.and.returnValue(throwError(() => new Error('Network error')));

    const fixture = TestBed.createComponent(FeatureFlagsComponent);
    fixture.detectChanges();
    tick();

    expect(fixture.componentInstance.loading()).toBeFalse();
  }));
});
