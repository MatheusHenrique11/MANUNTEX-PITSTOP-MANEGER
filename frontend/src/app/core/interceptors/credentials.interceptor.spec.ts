import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { credentialsInterceptor } from './credentials.interceptor';

describe('credentialsInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([credentialsInterceptor])),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deve adicionar withCredentials: true em todas as requisições', () => {
    http.get('/api/v1/veiculos').subscribe();

    const req = httpMock.expectOne('/api/v1/veiculos');
    expect(req.request.withCredentials).toBeTrue();
    req.flush([]);
  });

  it('deve preservar o método HTTP original', () => {
    http.post('/api/v1/auth/login', { email: 'a@b.com' }).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBeTrue();
    req.flush({});
  });
});
