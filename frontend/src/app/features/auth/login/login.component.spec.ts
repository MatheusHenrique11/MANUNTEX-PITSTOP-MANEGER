import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';

describe('LoginComponent', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, RouterTestingModule, ReactiveFormsModule],
      providers: [{ provide: AuthService, useValue: authServiceSpy }],
    }).compileComponents();
  });

  function createComponent() {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('deve criar o componente', () => {
    const fixture = createComponent();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('deve iniciar com formulário inválido', () => {
    const fixture = createComponent();
    expect(fixture.componentInstance.form.invalid).toBeTrue();
  });

  it('deve validar email inválido', () => {
    const fixture = createComponent();
    const comp = fixture.componentInstance;

    comp.form.get('email')!.setValue('nao_e_email');
    comp.form.get('email')!.markAsTouched();
    fixture.detectChanges();

    expect(comp.form.get('email')!.invalid).toBeTrue();
  });

  it('deve rejeitar senha com menos de 8 caracteres', () => {
    const fixture = createComponent();
    const comp = fixture.componentInstance;

    comp.form.get('password')!.setValue('123');
    comp.form.get('password')!.markAsTouched();

    expect(comp.form.get('password')!.invalid).toBeTrue();
  });

  it('deve chamar authService.login ao submeter formulário válido', fakeAsync(() => {
    authServiceSpy.login.and.returnValue(of({ expiresIn: 900, role: 'ROLE_ADMIN', accessToken: null as any }));
    const fixture = createComponent();
    const comp = fixture.componentInstance;

    comp.form.get('email')!.setValue('admin@test.com');
    comp.form.get('password')!.setValue('senha1234');
    comp.submit();
    tick();

    expect(authServiceSpy.login).toHaveBeenCalledWith({
      email: 'admin@test.com',
      password: 'senha1234',
    });
  }));

  it('não deve chamar login com formulário inválido', () => {
    const fixture = createComponent();
    const comp = fixture.componentInstance;

    comp.form.get('email')!.setValue('invalido');
    comp.submit();

    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('deve exibir mensagem de erro em falha de login', fakeAsync(() => {
    authServiceSpy.login.and.returnValue(
      throwError(() => ({ detail: 'Credenciais inválidas' }))
    );
    const fixture = createComponent();
    const comp = fixture.componentInstance;

    comp.form.get('email')!.setValue('user@test.com');
    comp.form.get('password')!.setValue('errada1234');
    comp.submit();
    tick();

    expect(comp.errorMessage()).toBe('Credenciais inválidas');
    expect(comp.loading()).toBeFalse();
  }));

  it('deve ativar estado de loading durante o login', fakeAsync(() => {
    authServiceSpy.login.and.returnValue(of({ expiresIn: 900, role: 'ROLE_ADMIN', accessToken: null as any }));
    const fixture = createComponent();
    const comp = fixture.componentInstance;

    comp.form.get('email')!.setValue('admin@test.com');
    comp.form.get('password')!.setValue('senha1234');

    comp.submit();
    expect(comp.loading()).toBeTrue();

    tick();
    expect(comp.loading()).toBeFalse();
  }));
});
