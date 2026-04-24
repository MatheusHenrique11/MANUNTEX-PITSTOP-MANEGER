import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MetaService } from '@core/services/meta.service';
import { UserAdminService } from '@core/services/user-admin.service';
import { ProducaoMecanicoResponse } from '@core/models/meta.model';
import { UserResponse } from '@core/models/user.model';
import { DefinirMetaDialogComponent } from './definir-meta-dialog.component';

@Component({
  selector: 'app-metas-gerente',
  standalone: true,
  imports: [CommonModule, FormsModule, DefinirMetaDialogComponent],
  template: `
    <div class="p-6 space-y-6">

      <!-- Cabeçalho -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 class="text-xl font-bold text-slate-100">Metas por Mecânico</h1>
          <p class="text-sm text-slate-500 mt-0.5">Acompanhe e gerencie a produção de cada mecânico</p>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <!-- Seletor de período -->
          <select [(ngModel)]="mesSelecionado" (ngModelChange)="carregar()"
            class="bg-surface-800 border border-surface-600 text-slate-300 text-sm rounded-lg
                   px-3 py-2 focus:outline-none focus:ring-2 focus:ring-petroleum-500">
            @for (m of meses; track m.valor) {
              <option [value]="m.valor">{{ m.label }}</option>
            }
          </select>
          <select [(ngModel)]="anoSelecionado" (ngModelChange)="carregar()"
            class="bg-surface-800 border border-surface-600 text-slate-300 text-sm rounded-lg
                   px-3 py-2 focus:outline-none focus:ring-2 focus:ring-petroleum-500">
            @for (a of anos; track a) {
              <option [value]="a">{{ a }}</option>
            }
          </select>
          <!-- Download PDF -->
          <button (click)="baixarPdf()" [disabled]="baixandoPdf()"
            class="flex items-center gap-2 px-4 py-2 text-sm font-semibold bg-surface-800
                   border border-surface-600 text-slate-300 hover:text-white hover:border-petroleum-500
                   rounded-lg transition-all disabled:opacity-50">
            @if (baixandoPdf()) {
              <span class="w-4 h-4 border-2 border-slate-400 border-t-transparent rounded-full animate-spin"></span>
            } @else {
              <span>📄</span>
            }
            Exportar PDF para RH
          </button>
        </div>
      </div>

      <!-- Loading -->
      @if (carregando()) {
        <div class="flex justify-center py-16">
          <div class="w-8 h-8 border-2 border-petroleum-500 border-t-transparent rounded-full animate-spin"></div>
        </div>
      }

      <!-- Erro -->
      @if (erro()) {
        <div class="bg-danger-600/10 border border-danger-600/30 rounded-lg p-4 text-danger-400 text-sm">
          {{ erro() }}
        </div>
      }

      <!-- Grid de mecânicos -->
      @if (!carregando() && producoes().length > 0) {
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          @for (p of producoes(); track p.mecanicoId) {
            <div class="bg-surface-900 border rounded-xl overflow-hidden transition-all hover:border-petroleum-600/50"
                 [ngClass]="{'border-emerald-600/40': p.metaBatida, 'border-surface-700': !p.metaBatida}">

              <!-- Header do card -->
              <div class="px-5 py-4 border-b"
                   [ngClass]="{'border-emerald-800/30': p.metaBatida, 'border-surface-700': !p.metaBatida}">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-3">
                    <div class="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0"
                         [ngClass]="p.metaBatida
                           ? {'bg-emerald-500/20': true, 'text-emerald-400': true}
                           : {'bg-surface-700': true, 'text-slate-400': true}">
                      {{ p.mecanicoNome[0] }}
                    </div>
                    <div>
                      <p class="text-sm font-semibold text-slate-200">{{ p.mecanicoNome }}</p>
                      <p class="text-xs text-slate-500">{{ p.totalServicos }} OS concluídas</p>
                    </div>
                  </div>
                  <span class="text-xs font-bold px-2 py-1 rounded-full"
                        [ngClass]="{
                          'bg-emerald-500/20': p.metaBatida,
                          'text-emerald-400': p.metaBatida,
                          'bg-surface-700': !p.metaBatida && p.valorMeta != null,
                          'text-slate-400': !p.metaBatida && p.valorMeta != null,
                          'bg-surface-800': p.valorMeta == null,
                          'text-slate-600': p.valorMeta == null
                        }">
                    {{ p.valorMeta == null ? 'Sem meta' : (p.metaBatida ? 'Atingiu' : 'Pendente') }}
                  </span>
                </div>
              </div>

              <!-- Corpo do card -->
              <div class="px-5 py-4 space-y-3">
                <!-- Valores -->
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <p class="text-xs text-slate-600 mb-0.5">Produzido</p>
                    <p class="text-base font-bold text-slate-200">
                      {{ p.totalValorProduzido | currency:'BRL':'symbol':'1.0-0':'pt-BR' }}
                    </p>
                  </div>
                  <div>
                    <p class="text-xs text-slate-600 mb-0.5">Meta</p>
                    <p class="text-base font-bold text-slate-200">
                      {{ p.valorMeta != null ? (p.valorMeta | currency:'BRL':'symbol':'1.0-0':'pt-BR') : '—' }}
                    </p>
                  </div>
                </div>

                <!-- Barra de progresso -->
                @if (p.valorMeta != null) {
                  <div>
                    <div class="flex justify-between text-xs text-slate-500 mb-1">
                      <span>{{ p.percentualAtingido | number:'1.1-1' }}%</span>
                    </div>
                    <div class="w-full bg-surface-700 rounded-full h-2">
                      <div class="h-2 rounded-full transition-all"
                           [class.bg-emerald-500]="p.metaBatida"
                           [class.bg-petroleum-500]="!p.metaBatida"
                           [style.width.%]="min(p.percentualAtingido, 100)">
                      </div>
                    </div>
                  </div>
                }

                <!-- Ações -->
                <div class="flex gap-2 pt-1">
                  <button (click)="verDetalhes(p)"
                    class="flex-1 text-xs py-1.5 rounded-lg bg-surface-800 hover:bg-surface-700
                           text-slate-400 hover:text-slate-200 border border-surface-600
                           hover:border-surface-500 transition-all">
                    Ver detalhes
                  </button>
                  <button (click)="abrirDefinirMeta(p)"
                    class="flex-1 text-xs py-1.5 rounded-lg bg-petroleum-700/20 hover:bg-petroleum-700/40
                           text-petroleum-400 hover:text-petroleum-300 border border-petroleum-700/30
                           hover:border-petroleum-600/50 transition-all">
                    {{ p.valorMeta != null ? 'Editar meta' : 'Definir meta' }}
                  </button>
                </div>
              </div>
            </div>
          }
        </div>
      }

      <!-- Estado vazio -->
      @if (!carregando() && producoes().length === 0 && !erro()) {
        <div class="bg-surface-900 border border-surface-700 rounded-xl p-10 text-center">
          <p class="text-slate-400 text-sm">Nenhum mecânico com produção ou meta definida neste período.</p>
          <p class="text-slate-600 text-xs mt-1">Defina metas para os mecânicos usando o botão em cada card.</p>
        </div>
      }

    </div>

    <!-- Dialog de definição de meta -->
    @if (mecanicoParaMeta()) {
      <app-definir-meta-dialog
        [mecanico]="mecanicoParaMeta()!"
        (metaSalva)="onMetaSalva()"
        (cancelado)="mecanicoParaMeta.set(null)">
      </app-definir-meta-dialog>
    }
  `,
})
export class MetasGerenteComponent implements OnInit {
  private metaService = inject(MetaService);
  private userService = inject(UserAdminService);
  private router = inject(Router);

  readonly producoes = signal<ProducaoMecanicoResponse[]>([]);
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly baixandoPdf = signal(false);
  readonly mecanicoParaMeta = signal<UserResponse | null>(null);

  mesSelecionado = new Date().getMonth() + 1;
  anoSelecionado = new Date().getFullYear();

  private mecanicos: UserResponse[] = [];

  readonly meses = [
    { valor: 1, label: 'Janeiro' }, { valor: 2, label: 'Fevereiro' },
    { valor: 3, label: 'Março' },   { valor: 4, label: 'Abril' },
    { valor: 5, label: 'Maio' },    { valor: 6, label: 'Junho' },
    { valor: 7, label: 'Julho' },   { valor: 8, label: 'Agosto' },
    { valor: 9, label: 'Setembro' },{ valor: 10, label: 'Outubro' },
    { valor: 11, label: 'Novembro' },{ valor: 12, label: 'Dezembro' },
  ];

  readonly anos = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i);

  ngOnInit() {
    this.userService.listar().subscribe({
      next: users => {
        this.mecanicos = users.filter(u => u.role === 'ROLE_MECANICO' && u.enabled);
      },
    });
    this.carregar();
  }

  carregar() {
    this.carregando.set(true);
    this.erro.set(null);
    this.metaService.listarProducaoGeral(this.mesSelecionado, this.anoSelecionado).subscribe({
      next: data => { this.producoes.set(data); this.carregando.set(false); },
      error: () => {
        this.erro.set('Não foi possível carregar os dados. Tente novamente.');
        this.carregando.set(false);
      },
    });
  }

  verDetalhes(p: ProducaoMecanicoResponse) {
    this.router.navigate(['/metas/gerente/mecanico', p.mecanicoId], {
      queryParams: { mes: this.mesSelecionado, ano: this.anoSelecionado },
    });
  }

  abrirDefinirMeta(p: ProducaoMecanicoResponse) {
    const mecanico = this.mecanicos.find(m => m.id === p.mecanicoId);
    if (mecanico) {
      this.mecanicoParaMeta.set(mecanico);
    }
  }

  onMetaSalva() {
    this.mecanicoParaMeta.set(null);
    this.carregar();
  }

  baixarPdf() {
    this.baixandoPdf.set(true);
    this.metaService.baixarPdfRelatorio(this.mesSelecionado, this.anoSelecionado).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `relatorio-metas-${String(this.mesSelecionado).padStart(2, '0')}-${this.anoSelecionado}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
        this.baixandoPdf.set(false);
      },
      error: () => {
        this.erro.set('Erro ao gerar PDF. Tente novamente.');
        this.baixandoPdf.set(false);
      },
    });
  }

  min(a: number, b: number): number {
    return Math.min(a, b);
  }
}
