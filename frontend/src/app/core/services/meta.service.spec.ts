import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MetaService } from './meta.service';
import { MetaRequest, MetaResponse, ProducaoMecanicoResponse } from '@core/models/meta.model';
import { environment } from '@env/environment';

describe('MetaService', () => {
  let service: MetaService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiUrl}/metas`;

  const mockMeta: MetaResponse = {
    id: 'meta-uuid', mecanicoId: 'mec-uuid', mecanicoNome: 'Carlos Silva',
    mes: 4, ano: 2026, valorMeta: 5000, createdAt: '', updatedAt: '',
  };

  const mockProducao: ProducaoMecanicoResponse = {
    mecanicoId: 'mec-uuid', mecanicoNome: 'Carlos Silva', mes: 4, ano: 2026,
    totalServicos: 3, totalValorProduzido: 5500, valorMeta: 5000,
    percentualAtingido: 110, metaBatida: true, servicos: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MetaService],
    });
    service = TestBed.inject(MetaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deve ser criado', () => {
    expect(service).toBeTruthy();
  });

  it('definirMeta deve fazer POST com o payload correto', () => {
    const req: MetaRequest = { mecanicoId: 'mec-uuid', mes: 4, ano: 2026, valorMeta: 5000 };
    service.definirMeta(req).subscribe(res => {
      expect(res.valorMeta).toBe(5000);
      expect(res.mecanicoNome).toBe('Carlos Silva');
    });

    const http = httpMock.expectOne(base);
    expect(http.request.method).toBe('POST');
    expect(http.request.body).toEqual(req);
    http.flush(mockMeta);
  });

  it('listarProducaoGeral deve fazer GET com mes e ano como params', () => {
    service.listarProducaoGeral(4, 2026).subscribe(res => {
      expect(res.length).toBe(1);
      expect(res[0].mecanicoNome).toBe('Carlos Silva');
    });

    const http = httpMock.expectOne(r => r.url === base && r.params.has('mes'));
    expect(http.request.method).toBe('GET');
    expect(http.request.params.get('mes')).toBe('4');
    expect(http.request.params.get('ano')).toBe('2026');
    http.flush([mockProducao]);
  });

  it('buscarMinhaProducao deve fazer GET em /minhas', () => {
    service.buscarMinhaProducao(4, 2026).subscribe(res => {
      expect(res.mecanicoId).toBe('mec-uuid');
    });

    const http = httpMock.expectOne(r => r.url === `${base}/minhas`);
    expect(http.request.method).toBe('GET');
    http.flush(mockProducao);
  });

  it('buscarProducaoMecanico deve fazer GET com o ID do mecânico', () => {
    service.buscarProducaoMecanico('mec-uuid', 4, 2026).subscribe(res => {
      expect(res.metaBatida).toBeTrue();
    });

    const http = httpMock.expectOne(r => r.url === `${base}/mecanico/mec-uuid`);
    expect(http.request.method).toBe('GET');
    http.flush(mockProducao);
  });

  it('buscarMeta deve fazer GET no endpoint de meta por mecânico e período', () => {
    service.buscarMeta('mec-uuid', 4, 2026).subscribe(res => {
      expect(res.valorMeta).toBe(5000);
    });

    const http = httpMock.expectOne(r => r.url === `${base}/mecanico/mec-uuid/meta`);
    expect(http.request.method).toBe('GET');
    http.flush(mockMeta);
  });

  it('baixarPdfRelatorio deve fazer GET com responseType blob', () => {
    const fakeBlob = new Blob(['%PDF'], { type: 'application/pdf' });
    service.baixarPdfRelatorio(4, 2026).subscribe(res => {
      expect(res instanceof Blob).toBeTrue();
    });

    const http = httpMock.expectOne(r => r.url === `${base}/relatorio/pdf`);
    expect(http.request.method).toBe('GET');
    expect(http.request.responseType).toBe('blob');
    http.flush(fakeBlob);
  });
});
