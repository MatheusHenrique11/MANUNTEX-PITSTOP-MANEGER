import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { VeiculoService } from './veiculo.service';
import { PageResponse, Veiculo } from '../models/veiculo.model';

describe('VeiculoService', () => {
  let service: VeiculoService;
  let httpMock: HttpTestingController;

  const mockVeiculo: Veiculo = {
    id: '123',
    placa: 'ABC1234',
    chassi: '*************4251', // mascarado
    renavam: '********590',       // mascarado
    marca: 'Volkswagen',
    modelo: 'Gol',
    anoFabricacao: 2020,
    anoModelo: 2021,
    clienteId: 'cliente-uuid',
    createdAt: new Date().toISOString(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [VeiculoService],
    });
    service = TestBed.inject(VeiculoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deve listar veículos com paginação padrão', fakeAsync(() => {
    const page: PageResponse<Veiculo> = {
      content: [mockVeiculo],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
    };

    let result: any;
    service.listar().subscribe(p => (result = p));

    const req = httpMock.expectOne(r =>
      r.url.includes('/veiculos') &&
      r.params.get('page') === '0' &&
      r.params.get('size') === '20'
    );
    expect(req.request.method).toBe('GET');
    req.flush(page);
    tick();

    expect(result.content.length).toBe(1);
    expect(result.content[0].placa).toBe('ABC1234');
  }));

  it('deve enviar parâmetro de busca quando fornecido', fakeAsync(() => {
    service.listar(0, 20, 'ABC').subscribe();

    const req = httpMock.expectOne(r =>
      r.url.includes('/veiculos') && r.params.get('q') === 'ABC'
    );
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 });
    tick();
  }));

  it('não deve enviar parâmetro q quando não fornecido', fakeAsync(() => {
    service.listar(0, 20).subscribe();

    const req = httpMock.expectOne(r => r.url.includes('/veiculos'));
    expect(req.request.params.has('q')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 });
    tick();
  }));

  it('deve buscar veículo por ID', fakeAsync(() => {
    let result: Veiculo | undefined;
    service.buscarPorId('123').subscribe(v => (result = v));

    const req = httpMock.expectOne(r => r.url.includes('/veiculos/123'));
    req.flush(mockVeiculo);
    tick();

    expect(result!.placa).toBe('ABC1234');
  }));

  it('deve criar novo veículo via POST', fakeAsync(() => {
    const payload = {
      placa: 'XYZ9876', chassi: '9BWZZZ377VT004251', renavam: '00258665590',
      marca: 'Fiat', modelo: 'Uno', anoFabricacao: 2019, anoModelo: 2020,
      clienteId: 'cliente-id',
    };

    service.criar(payload as any).subscribe();

    const req = httpMock.expectOne(r => r.url.includes('/veiculos'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.placa).toBe('XYZ9876');
    req.flush(mockVeiculo);
    tick();
  }));
});
