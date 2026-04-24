import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MetaService } from '@core/services/meta.service';
import { ProducaoMecanicoResponse } from '@core/models/meta.model';

@Component({
  selector: 'app-detalhe-mecanico',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="p-6 max-w-5xl mx-auto space-y-6">

      <!-- Voltar -->
      <a routerLink="/metas/gerente"
         class="inline-flex items-center gap-2 text-sm text-slate-500 hover:text-slate-300 transition-colors">
        ← Voltar para todos os mecânicos
      </a>

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

      @if (producao(); as p) {
        <!-- Header do mecânico -->
        <div class="bg-surface-900 border rounded-xl p-6"
             [ngClass]="{'border-emerald-700/40': p.metaBatida, 'border-surface-700': !p.metaBatida}">
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div class="flex items-center gap-4">
              <div class="w-14 h-14 rounded-full flex items-center justify-center text-xl font-bold flex-shrink-0"
                   [ngClass]="p.metaBatida
                     ? {'bg-emerald-500/20': true, 'text-emerald-400': true}
                     : {'bg-surface-700': true, 'text-slate-300': true}">
                {{ p.mecanicoNome[0] }}
              </div>
              <div>
                <h1 class="text-xl font-bold text-slate-100">{{ p.mecanicoNome }}</h1>
                <p class="text-sm text-slate-500">
                  {{ nomeMes(p.mes) }} de {{ p.ano }}
                </p>
              </div>
            </div>
            <span class="text-sm font-bold px-4 py-2 rounded-full self-start sm:self-auto border"
                  [ngClass]="p.metaBatida
                    ? {'bg-emerald-500/20': true, 'text-emerald-400': true, 'border-emerald-600/40': true}
                    : {'bg-surface-800': true, 'text-slate-400': true, 'border-surface-600': true}">
              {{ p.metaBatida ? '✓ Meta Atingida' : 'Meta Pendente' }}
            </span>
          </div>
        </div>

        <!-- KPIs -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-4">
            <p class="text-xs text-slate-500 uppercase tracking-widest mb-1">OS Concluídas</p>
            <p class="text-3xl font-bold text-slate-100">{{ p.totalServicos }}</p>
          </div>
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-4">
            <p class="text-xs text-slate-500 uppercase tracking-widest mb-1">Total Produzido</p>
            <p class="text-2xl font-bold text-slate-100">
              {{ p.totalValorProduzido | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
            </p>
          </div>
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-4">
            <p class="text-xs text-slate-500 uppercase tracking-widest mb-1">Meta</p>
            <p class="text-2xl font-bold text-slate-100">
              {{ p.valorMeta != null ? (p.valorMeta | currency:'BRL':'symbol':'1.2-2':'pt-BR') : '—' }}
            </p>
          </div>
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-4">
            <p class="text-xs text-slate-500 uppercase tracking-widest mb-1">% Atingido</p>
            <p class="text-3xl font-bold"
               [class.text-emerald-400]="p.metaBatida"
               [class.text-safety-400]="!p.metaBatida && p.valorMeta != null"
               [class.text-slate-400]="p.valorMeta == null">
              {{ p.valorMeta != null ? (p.percentualAtingido | number:'1.1-1') + '%' : '—' }}
            </p>
          </div>
        </div>

        <!-- Barra de progresso -->
        @if (p.valorMeta != null) {
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-5">
            <div class="flex justify-between items-center mb-3">
              <span class="text-sm font-medium text-slate-300">Progresso da Meta</span>
              <span class="text-sm text-slate-400">
                {{ p.totalValorProduzido | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
                /
                {{ p.valorMeta | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
              </span>
            </div>
            <div class="w-full bg-surface-700 rounded-full h-3">
              <div class="h-3 rounded-full transition-all duration-700"
                   [class.bg-emerald-500]="p.metaBatida"
                   [class.bg-petroleum-500]="!p.metaBatida"
                   [style.width.%]="min(p.percentualAtingido, 100)">
              </div>
            </div>
          </div>
        }

        <!-- Tabela completa de serviços -->
        <div class="bg-surface-900 rounded-xl border border-surface-700 overflow-hidden">
          <div class="px-5 py-4 border-b border-surface-700 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-slate-300">Ordens de Serviço Concluídas</h2>
            <span class="text-xs text-slate-600">{{ p.servicos.length }} registro(s)</span>
          </div>

          @if (p.servicos.length > 0) {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-surface-700 bg-surface-800/50">
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Placa</th>
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Veículo</th>
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Cliente</th>
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide hidden lg:table-cell">Descrição</th>
                    <th class="px-5 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wide">Valor Final</th>
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide hidden md:table-cell">Conclusão</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-surface-800">
                  @for (s of p.servicos; track s.manutencaoId; let par = $even) {
                    <tr [ngClass]="{'bg-surface-800/20': par}" class="hover:bg-surface-800/50 transition-colors">
                      <td class="px-5 py-3 font-mono text-sm text-slate-300">{{ s.veiculoPlaca }}</td>
                      <td class="px-5 py-3 text-slate-300">{{ s.veiculoMarca }} {{ s.veiculoModelo }}</td>
                      <td class="px-5 py-3 text-slate-400">{{ s.clienteNome ?? '—' }}</td>
                      <td class="px-5 py-3 text-slate-400 hidden lg:table-cell max-w-xs">
                        <span class="line-clamp-1">{{ s.descricao }}</span>
                      </td>
                      <td class="px-5 py-3 text-right font-bold text-emerald-400">
                        {{ s.valorFinal != null ? (s.valorFinal | currency:'BRL':'symbol':'1.2-2':'pt-BR') : '—' }}
                      </td>
                      <td class="px-5 py-3 text-slate-400 hidden md:table-cell">
                        {{ s.dataConclusao ? (s.dataConclusao | date:'dd/MM/yyyy':'':'pt-BR') : '—' }}
                      </td>
                    </tr>
                  }
                </tbody>
                <tfoot class="border-t border-surface-600 bg-surface-800/50">
                  <tr>
                    <td colspan="4" class="px-5 py-3 text-sm font-semibold text-slate-400">Total</td>
                    <td class="px-5 py-3 text-right font-bold text-emerald-400">
                      {{ p.totalValorProduzido | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
                    </td>
                    <td class="hidden md:table-cell"></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          } @else {
            <div class="p-8 text-center">
              <p class="text-slate-500 text-sm">Nenhum serviço concluído neste período.</p>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class DetalheMecanicoComponent implements OnInit {
  private metaService = inject(MetaService);
  private route = inject(ActivatedRoute);

  readonly producao = signal<ProducaoMecanicoResponse | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);

  private mecanicoId = '';
  private mes = new Date().getMonth() + 1;
  private ano = new Date().getFullYear();

  private readonly MESES = [
    'Janeiro','Fevereiro','Março','Abril','Maio','Junho',
    'Julho','Agosto','Setembro','Outubro','Novembro','Dezembro',
  ];

  ngOnInit() {
    this.mecanicoId = this.route.snapshot.paramMap.get('mecanicoId') ?? '';
    this.mes = Number(this.route.snapshot.queryParamMap.get('mes')) || this.mes;
    this.ano = Number(this.route.snapshot.queryParamMap.get('ano')) || this.ano;
    this.carregar();
  }

  private carregar() {
    if (!this.mecanicoId) return;
    this.carregando.set(true);
    this.metaService.buscarProducaoMecanico(this.mecanicoId, this.mes, this.ano).subscribe({
      next: data => { this.producao.set(data); this.carregando.set(false); },
      error: () => {
        this.erro.set('Não foi possível carregar os dados do mecânico.');
        this.carregando.set(false);
      },
    });
  }

  nomeMes(mes: number): string {
    return this.MESES[mes - 1] ?? '';
  }

  min(a: number, b: number): number {
    return Math.min(a, b);
  }
}
