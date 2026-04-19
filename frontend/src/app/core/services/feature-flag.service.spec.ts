import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FeatureFlagService } from './feature-flag.service';
import { FeatureFlagsMap } from '../models/feature-flag.model';

describe('FeatureFlagService', () => {
  let service: FeatureFlagService;
  let httpMock: HttpTestingController;

  const mockFlags: FeatureFlagsMap = {
    VEHICLE_MANAGEMENT:  { active: true,  label: 'Gestão de Veículos' },
    DOCUMENT_VAULT:      { active: true,  label: 'Cofre de Documentos' },
    MAINTENANCE_MODULE:  { active: false, label: 'Manutenções' },
    ANALYTICS_DASHBOARD: { active: false, label: 'Dashboard' },
    NOTIFICATIONS:       { active: false, label: 'Notificações' },
    FINANCIAL_MODULE:    { active: false, label: 'Financeiro' },
    DETRAN_INTEGRATION:  { active: false, label: 'DETRAN' },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [FeatureFlagService],
    });
    service = TestBed.inject(FeatureFlagService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deve retornar false antes de carregar flags', () => {
    expect(service.isActive('VEHICLE_MANAGEMENT')).toBeFalse();
  });

  it('deve carregar flags do backend', fakeAsync(() => {
    service.load().subscribe();

    const req = httpMock.expectOne(req => req.url.includes('/features'));
    expect(req.request.method).toBe('GET');
    req.flush(mockFlags);
    tick();

    expect(service.isActive('VEHICLE_MANAGEMENT')).toBeTrue();
    expect(service.isActive('MAINTENANCE_MODULE')).toBeFalse();
  }));

  it('deve atualizar o signal flags após carregar', fakeAsync(() => {
    service.load().subscribe();
    httpMock.expectOne(req => req.url.includes('/features')).flush(mockFlags);
    tick();

    const flags = service.flags();
    expect(flags).not.toBeNull();
    expect(flags!['DOCUMENT_VAULT'].label).toBe('Cofre de Documentos');
  }));

  it('deve retornar false para feature desconhecida', fakeAsync(() => {
    service.load().subscribe();
    httpMock.expectOne(req => req.url.includes('/features')).flush(mockFlags);
    tick();

    expect(service.isActive('FEATURE_INEXISTENTE' as any)).toBeFalse();
  }));
});
