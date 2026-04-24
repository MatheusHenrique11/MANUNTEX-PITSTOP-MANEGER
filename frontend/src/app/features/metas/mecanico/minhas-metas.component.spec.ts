import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MinhasMetasComponent } from './minhas-metas.component';
import { MetaService } from '@core/services/meta.service';
import { of, throwError } from 'rxjs';
import { ProducaoMecanicoResponse } from '@core/models/meta.model';
import { By } from '@angular/platform-browser';

const PRODUCAO_MOCK: ProducaoMecanicoResponse = {
  mecanicoId: 'mec-1', mecanicoNome: 'Carlos Silva', mes: 4, ano: 2026,
  totalServicos: 2, totalValorProduzido: 5500, valorMeta: 5000,
  percentualAtingido: 110, metaBatida: true,
  servicos: [
    {
      manutencaoId: 'os-1', veiculoPlaca: 'ABC1D23', veiculoMarca: 'VW',
      veiculoModelo: 'Gol', clienteNome: 'João', descricao: 'Revisão de motor',
      valorFinal: 3000, dataConclusao: '2026-04-10T10:00:00Z',
      status: 'CONCLUIDA' as any,
    },
  ],
};

describe('MinhasMetasComponent', () => {
  let fixture: ComponentFixture<MinhasMetasComponent>;
  let metaServiceSpy: jasmine.SpyObj<MetaService>;

  beforeEach(async () => {
    metaServiceSpy = jasmine.createSpyObj('MetaService', ['buscarMinhaProducao']);
    metaServiceSpy.buscarMinhaProducao.and.returnValue(of(PRODUCAO_MOCK));

    await TestBed.configureTestingModule({
      imports: [MinhasMetasComponent],
      providers: [{ provide: MetaService, useValue: metaServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(MinhasMetasComponent);
    fixture.detectChanges();
  });

  it('deve ser criado', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('deve chamar buscarMinhaProducao ao inicializar', () => {
    expect(metaServiceSpy.buscarMinhaProducao).toHaveBeenCalled();
  });

  it('deve exibir total de ordens concluídas', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('2');
  });

  it('deve indicar meta atingida', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Meta atingida');
  });

  it('deve exibir a placa do veículo na tabela de serviços', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('ABC1D23');
  });

  it('deve exibir mensagem de erro quando o serviço falha', async () => {
    metaServiceSpy.buscarMinhaProducao.and.returnValue(
      throwError(() => new Error('Server error'))
    );
    fixture.componentInstance.carregar();
    fixture.detectChanges();
    await fixture.whenStable();

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Não foi possível carregar');
  });

  it('progressoExibido não deve ultrapassar 100', () => {
    const comp = fixture.componentInstance;
    expect(comp.progressoExibido(150)).toBe(100);
    expect(comp.progressoExibido(80)).toBe(80);
  });
});
