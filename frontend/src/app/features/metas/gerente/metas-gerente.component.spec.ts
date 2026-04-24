import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MetasGerenteComponent } from './metas-gerente.component';
import { MetaService } from '@core/services/meta.service';
import { UserAdminService } from '@core/services/user-admin.service';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { ProducaoMecanicoResponse } from '@core/models/meta.model';

const PRODUCAO_MOCK: ProducaoMecanicoResponse[] = [
  {
    mecanicoId: 'mec-1', mecanicoNome: 'Carlos Silva', mes: 4, ano: 2026,
    totalServicos: 3, totalValorProduzido: 5500, valorMeta: 5000,
    percentualAtingido: 110, metaBatida: true, servicos: [],
  },
  {
    mecanicoId: 'mec-2', mecanicoNome: 'Pedro Mecânico', mes: 4, ano: 2026,
    totalServicos: 1, totalValorProduzido: 800, valorMeta: 5000,
    percentualAtingido: 16, metaBatida: false, servicos: [],
  },
];

describe('MetasGerenteComponent', () => {
  let fixture: ComponentFixture<MetasGerenteComponent>;
  let metaServiceSpy: jasmine.SpyObj<MetaService>;
  let userServiceSpy: jasmine.SpyObj<UserAdminService>;

  beforeEach(async () => {
    metaServiceSpy = jasmine.createSpyObj('MetaService', [
      'listarProducaoGeral', 'baixarPdfRelatorio',
    ]);
    metaServiceSpy.listarProducaoGeral.and.returnValue(of(PRODUCAO_MOCK));

    userServiceSpy = jasmine.createSpyObj('UserAdminService', ['listar']);
    userServiceSpy.listar.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [MetasGerenteComponent, RouterTestingModule],
      providers: [
        { provide: MetaService, useValue: metaServiceSpy },
        { provide: UserAdminService, useValue: userServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MetasGerenteComponent);
    fixture.detectChanges();
  });

  it('deve ser criado', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('deve chamar listarProducaoGeral ao inicializar', () => {
    expect(metaServiceSpy.listarProducaoGeral).toHaveBeenCalled();
  });

  it('deve exibir os nomes dos mecânicos', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Carlos Silva');
    expect(texto).toContain('Pedro Mecânico');
  });

  it('deve mostrar status "Atingiu" para mecânico que bateu meta', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Atingiu');
  });

  it('deve mostrar status "Pendente" para mecânico que não bateu meta', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Pendente');
  });

  it('deve exibir erro quando o serviço falha', async () => {
    metaServiceSpy.listarProducaoGeral.and.returnValue(
      throwError(() => new Error('Server error'))
    );
    fixture.componentInstance.carregar();
    fixture.detectChanges();
    await fixture.whenStable();

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Não foi possível carregar');
  });

  it('deve chamar baixarPdfRelatorio ao clicar em Exportar PDF', () => {
    const fakeBlob = new Blob(['%PDF'], { type: 'application/pdf' });
    metaServiceSpy.baixarPdfRelatorio = jasmine.createSpy().and.returnValue(of(fakeBlob));

    spyOn(URL, 'createObjectURL').and.returnValue('blob:fake');
    spyOn(URL, 'revokeObjectURL');
    spyOn(document, 'createElement').and.callFake((tag: string) => {
      if (tag === 'a') {
        return { href: '', download: '', click: jasmine.createSpy() } as any;
      }
      return document.createElement(tag);
    });

    fixture.componentInstance.baixarPdf();
    expect(metaServiceSpy.baixarPdfRelatorio).toHaveBeenCalled();
  });

  it('min() não deve ultrapassar o segundo argumento', () => {
    expect(fixture.componentInstance.min(150, 100)).toBe(100);
    expect(fixture.componentInstance.min(50, 100)).toBe(50);
  });
});
