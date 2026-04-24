import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MetaService } from '@core/services/meta.service';
import { ProducaoMecanicoResponse } from '@core/models/meta.model';

@Component({
  selector: 'app-minhas-metas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="p-6 max-w-4xl mx-auto space-y-6">

      <!-- Cabeçalho -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 class="text-xl font-bold text-slate-100">Minhas Metas</h1>
          <p class="text-sm text-slate-500 mt-0.5">Acompanhe sua produção e metas mensais</p>
        </div>
        <!-- Seletor de período -->
        <div class="flex items-center gap-2">
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
        </div>
      </div>

      <!-- Loading -->
      @if (carregando()) {
        <div class="flex justify-center py-12">
          <div class="w-8 h-8 border-2 border-petroleum-500 border-t-transparent rounded-full animate-spin"></div>
        </div>
      }

      <!-- Erro -->
      @if (erro()) {
        <div class="bg-danger-600/10 border border-danger-600/30 rounded-lg p-4 text-danger-400 text-sm">
          {{ erro() }}
        </div>
      }

      <!-- Dados -->
      @if (producao(); as p) {
        <!-- Card de resumo -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-4">
            <p class="text-xs text-slate-500 uppercase tracking-widest mb-1">Ordens Concluídas</p>
            <p class="text-2xl font-bold text-slate-100">{{ p.totalServicos }}</p>
          </div>
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-4">
            <p class="text-xs text-slate-500 uppercase tracking-widest mb-1">Total Produzido</p>
            <p class="text-2xl font-bold text-slate-100">{{ p.totalValorProduzido | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</p>
          </div>
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-4">
            <p class="text-xs text-slate-500 uppercase tracking-widest mb-1">Meta do Mês</p>
            <p class="text-2xl font-bold text-slate-100">
              {{ p.valorMeta != null ? (p.valorMeta | currency:'BRL':'symbol':'1.2-2':'pt-BR') : '—' }}
            </p>
          </div>
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-4">
            <p class="text-xs text-slate-500 uppercase tracking-widest mb-1">% Atingido</p>
            <p class="text-2xl font-bold"
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
              <span class="text-sm font-bold px-2 py-0.5 rounded-full"
                    [ngClass]="p.metaBatida
                      ? {'bg-emerald-500/20': true, 'text-emerald-400': true}
                      : {'bg-safety-500/20': true,  'text-safety-400':  true}">
                {{ p.metaBatida ? 'Meta atingida!' : 'Em andamento' }}
              </span>
            </div>
            <div class="w-full bg-surface-700 rounded-full h-3">
              <div class="h-3 rounded-full transition-all duration-700"
                   [class.bg-emerald-500]="p.metaBatida"
                   [class.bg-petroleum-500]="!p.metaBatida"
                   [style.width.%]="progressoExibido(p.percentualAtingido)">
              </div>
            </div>
            <div class="flex justify-between mt-2 text-xs text-slate-500">
              <span>R$ 0</span>
              <span>{{ p.valorMeta | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
            </div>
          </div>
        }

        <!-- Tabela de serviços -->
        @if (p.servicos.length > 0) {
          <div class="bg-surface-900 rounded-xl border border-surface-700 overflow-hidden">
            <div class="px-5 py-4 border-b border-surface-700">
              <h2 class="text-sm font-semibold text-slate-300">Serviços Concluídos</h2>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-surface-700">
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Placa</th>
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Veículo</th>
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide hidden md:table-cell">Descrição</th>
                    <th class="px-5 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wide">Valor</th>
                    <th class="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide hidden sm:table-cell">Conclusão</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-surface-800">
                  @for (s of p.servicos; track s.manutencaoId) {
                    <tr class="hover:bg-surface-800/50 transition-colors">
                      <td class="px-5 py-3 font-mono text-slate-300">{{ s.veiculoPlaca }}</td>
                      <td class="px-5 py-3 text-slate-300">{{ s.veiculoMarca }} {{ s.veiculoModelo }}</td>
                      <td class="px-5 py-3 text-slate-400 hidden md:table-cell max-w-xs truncate">{{ s.descricao }}</td>
                      <td class="px-5 py-3 text-right font-semibold text-emerald-400">
                        {{ s.valorFinal != null ? (s.valorFinal | currency:'BRL':'symbol':'1.2-2':'pt-BR') : '—' }}
                      </td>
                      <td class="px-5 py-3 text-slate-400 hidden sm:table-cell">
                        {{ s.dataConclusao ? (s.dataConclusao | date:'dd/MM/yyyy') : '—' }}
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        } @else {
          <div class="bg-surface-900 rounded-xl border border-surface-700 p-8 text-center">
            <p class="text-slate-500 text-sm">Nenhum serviço concluído neste período.</p>
          </div>
        }
      }

    </div>
  `,
})
export class MinhasMetasComponent implements OnInit {
  private metaService = inject(MetaService);

  readonly producao = signal<ProducaoMecanicoResponse | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);

  mesSelecionado = new Date().getMonth() + 1;
  anoSelecionado = new Date().getFullYear();

  readonly meses = [
    { valor: 1, label: 'Janeiro' }, { valor: 2, label: 'Fevereiro' },
    { valor: 3, label: 'Março' },   { valor: 4, label: 'Abril' },
    { valor: 5, label: 'Maio' },    { valor: 6, label: 'Junho' },
    { valor: 7, label: 'Julho' },   { valor: 8, label: 'Agosto' },
    { valor: 9, label: 'Setembro' },{ valor: 10, label: 'Outubro' },
    { valor: 11, label: 'Novembro' },{ valor: 12, label: 'Dezembro' },
  ];

  readonly anos = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i);

  ngOnInit() { this.carregar(); }

  carregar() {
    this.carregando.set(true);
    this.erro.set(null);
    this.metaService.buscarMinhaProducao(this.mesSelecionado, this.anoSelecionado).subscribe({
      next: data => { this.producao.set(data); this.carregando.set(false); },
      error: () => {
        this.erro.set('Não foi possível carregar seus dados. Tente novamente.');
        this.carregando.set(false);
      },
    });
  }

  progressoExibido(percentual: number): number {
    return Math.min(percentual, 100);
  }
}
