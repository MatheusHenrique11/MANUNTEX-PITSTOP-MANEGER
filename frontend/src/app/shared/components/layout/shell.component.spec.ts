import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { signal, computed } from '@angular/core';
import { ShellComponent } from './shell.component';
import { AuthService } from '@core/services/auth.service';
import { FeatureFlagService } from '@core/services/feature-flag.service';

describe('ShellComponent', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;
  let mockIsAdmin: ReturnType<typeof signal<boolean>>;
  let featureFlagSpy: jasmine.SpyObj<FeatureFlagService>;

  function buildAuthService(role: string) {
    return {
      isAdmin: computed(() => role === 'ROLE_ADMIN'),
      isAuthenticated: signal(true),
      role: signal(role),
      email: signal(`${role.toLowerCase()}@pitstop.com`),
      logout: jasmine.createSpy('logout'),
    };
  }

  async function setup(role: string, isActiveResult = true) {
    featureFlagSpy = jasmine.createSpyObj('FeatureFlagService', ['isActive']);
    featureFlagSpy.isActive.and.returnValue(isActiveResult);

    await TestBed.configureTestingModule({
      imports: [ShellComponent, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: buildAuthService(role) },
        { provide: FeatureFlagService, useValue: featureFlagSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('visibleNavItems() — admin', () => {
    beforeEach(async () => setup('ROLE_ADMIN', false));

    it('deve criar o componente', () => {
      expect(component).toBeTruthy();
    });

    it('deve exibir Dashboard para admin', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).toContain('Dashboard');
    });

    it('deve exibir Veículos para admin mesmo com módulo desativado', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).toContain('Veículos');
    });

    it('deve exibir Documentos para admin mesmo com módulo desativado', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).toContain('Documentos');
    });

    it('deve exibir itens adminOnly para admin', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).toContain('Usuários');
      expect(labels).toContain('Módulos');
    });

    it('não deve chamar isActive para admin (sem filtro por feature flag)', () => {
      component.visibleNavItems();
      expect(featureFlagSpy.isActive).not.toHaveBeenCalled();
    });
  });

  describe('visibleNavItems() — não-admin com módulos desativados', () => {
    beforeEach(async () => setup('ROLE_MECANICO', false));

    it('deve exibir Dashboard mesmo sem feature flag', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).toContain('Dashboard');
    });

    it('deve ocultar Veículos quando VEHICLE_MANAGEMENT está desativado', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).not.toContain('Veículos');
    });

    it('deve ocultar Documentos quando DOCUMENT_VAULT está desativado', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).not.toContain('Documentos');
    });

    it('deve ocultar itens adminOnly para não-admin', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).not.toContain('Usuários');
      expect(labels).not.toContain('Módulos');
    });

    it('deve chamar isActive para itens com feature', () => {
      component.visibleNavItems();
      expect(featureFlagSpy.isActive).toHaveBeenCalled();
    });
  });

  describe('visibleNavItems() — não-admin com módulos ativos', () => {
    beforeEach(async () => setup('ROLE_GERENTE', true));

    it('deve exibir itens de módulos ativos para não-admin', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).toContain('Veículos');
      expect(labels).toContain('Documentos');
    });

    it('deve continuar ocultando itens adminOnly', () => {
      const labels = component.visibleNavItems().map(i => i.label);
      expect(labels).not.toContain('Usuários');
    });
  });

  describe('roleLabel()', () => {
    it('deve retornar Administrador para ROLE_ADMIN', async () => {
      await setup('ROLE_ADMIN');
      expect(component.roleLabel()).toBe('Administrador');
    });

    it('deve retornar Gerente para ROLE_GERENTE', async () => {
      await setup('ROLE_GERENTE');
      expect(component.roleLabel()).toBe('Gerente');
    });

    it('deve retornar Mecânico para ROLE_MECANICO', async () => {
      await setup('ROLE_MECANICO');
      expect(component.roleLabel()).toBe('Mecânico');
    });

    it('deve retornar Recepcionista para ROLE_RECEPCIONISTA', async () => {
      await setup('ROLE_RECEPCIONISTA');
      expect(component.roleLabel()).toBe('Recepcionista');
    });

    it('deve retornar Usuário para role desconhecida', async () => {
      await setup('ROLE_UNKNOWN');
      expect(component.roleLabel()).toBe('Usuário');
    });
  });

  describe('userInitial()', () => {
    it('deve retornar a primeira letra do email em maiúsculo', async () => {
      await setup('ROLE_ADMIN');
      expect(component.userInitial()).toBe('R');
    });
  });
});
