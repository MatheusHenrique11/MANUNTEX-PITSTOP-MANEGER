import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from './auth.service';
import { AuthResponse } from '../models/auth.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockResponse: AuthResponse = {
    expiresIn: 900,
    role: 'ROLE_ADMIN',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AuthService],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // Consome a chamada de tryRestoreSession feita no constructor
    const refreshReq = httpMock.expectOne(req => req.url.includes('/auth/refresh'));
    refreshReq.error(new ErrorEvent('Network error'), { status: 401 });
  });

  afterEach(() => httpMock.verify());

  it('deve iniciar como não autenticado quando refresh falha', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.role()).toBeNull();
  });

  it('deve atualizar estado após login bem-sucedido', fakeAsync(() => {
    service.login({ email: 'admin@test.com', password: 'senha1234' }).subscribe();

    const req = httpMock.expectOne(req => req.url.includes('/auth/login'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@test.com', password: 'senha1234' });
    req.flush(mockResponse);

    tick();

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.role()).toBe('ROLE_ADMIN');
    expect(service.isAdmin()).toBeTrue();
  }));

  it('deve retornar isPrivileged true para ROLE_GERENTE', fakeAsync(() => {
    const gerenteResp: AuthResponse = { expiresIn: 900, role: 'ROLE_GERENTE' };
    service.login({ email: 'gerente@test.com', password: 'senha1234' }).subscribe();

    const req = httpMock.expectOne(req => req.url.includes('/auth/login'));
    req.flush(gerenteResp);
    tick();

    expect(service.isPrivileged()).toBeTrue();
    expect(service.isAdmin()).toBeFalse();
  }));

  it('deve limpar estado após logout', fakeAsync(() => {
    // Primeiro faz login
    service.login({ email: 'admin@test.com', password: 'senha' }).subscribe();
    httpMock.expectOne(req => req.url.includes('/auth/login')).flush(mockResponse);
    tick();

    expect(service.isAuthenticated()).toBeTrue();

    // Depois faz logout
    service.logout();
    httpMock.expectOne(req => req.url.includes('/auth/logout')).flush(null);
    tick();

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.role()).toBeNull();
  }));

  it('deve enviar withCredentials true no refresh', fakeAsync(() => {
    service.refreshToken().subscribe();
    const req = httpMock.expectOne(req => req.url.includes('/auth/refresh'));
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockResponse);
    tick();
  }));
});
