import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deve normalizar erro 401 para formato ProblemDetail', (done) => {
    http.get('/api/v1/veiculos').subscribe({
      error: (err) => {
        expect(err.status).toBe(401);
        expect(err.title).toBe('Não autorizado');
        expect(err.detail).toBeTruthy();
        done();
      },
    });

    httpMock.expectOne('/api/v1/veiculos').flush(
      { title: 'Não autorizado', detail: 'Sessão expirada' },
      { status: 401, statusText: 'Unauthorized' }
    );
  });

  it('deve usar mensagem padrão quando backend não retorna detail', (done) => {
    http.get('/api/v1/algo').subscribe({
      error: (err) => {
        expect(err.detail).toBe('Tente novamente. Se o problema persistir, contate o suporte.');
        done();
      },
    });

    httpMock.expectOne('/api/v1/algo').flush(
      {},
      { status: 500, statusText: 'Internal Server Error' }
    );
  });

  it('deve repassar erros de validação com campos', (done) => {
    http.post('/api/v1/veiculos', {}).subscribe({
      error: (err) => {
        expect(err.status).toBe(422);
        expect(err.fields).toEqual({ placa: 'Placa inválida' });
        done();
      },
    });

    httpMock.expectOne('/api/v1/veiculos').flush(
      {
        title: 'Dados inválidos',
        detail: 'Campos inválidos',
        fields: { placa: 'Placa inválida' },
      },
      { status: 422, statusText: 'Unprocessable Entity' }
    );
  });
});
