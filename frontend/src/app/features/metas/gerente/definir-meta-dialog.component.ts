import { Component, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MetaService } from '@core/services/meta.service';
import { UserResponse } from '@core/models/user.model';

@Component({
  selector: 'app-definir-meta-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <!-- Backdrop -->
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/70"
         (click)="fechar()">
      <div class="bg-surface-900 border border-surface-700 rounded-2xl shadow-2xl w-full max-w-md mx-4 p-6"
           (click)="$event.stopPropagation()">

        <h2 class="text-lg font-bold text-slate-100 mb-1">Definir Meta</h2>
        <p class="text-sm text-slate-500 mb-5">
          Mecânico: <span class="text-slate-300 font-medium">{{ mecanico().fullName }}</span>
        </p>

        <div class="space-y-4">
          <!-- Período -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-slate-400 mb-1.5 uppercase tracking-wide">Mês</label>
              <select [(ngModel)]="mes"
                class="w-full bg-surface-800 border border-surface-600 text-slate-300 text-sm
                       rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-petroleum-500">
                @for (m of meses; track m.valor) {
                  <option [value]="m.valor">{{ m.label }}</option>
                }
              </select>
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-400 mb-1.5 uppercase tracking-wide">Ano</label>
              <select [(ngModel)]="ano"
                class="w-full bg-surface-800 border border-surface-600 text-slate-300 text-sm
                       rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-petroleum-500">
                @for (a of anos; track a) {
                  <option [value]="a">{{ a }}</option>
                }
              </select>
            </div>
          </div>

          <!-- Valor da meta -->
          <div>
            <label class="block text-xs font-semibold text-slate-400 mb-1.5 uppercase tracking-wide">
              Valor da Meta (R$)
            </label>
            <input type="number" [(ngModel)]="valorMeta" min="0.01" step="100"
              placeholder="Ex: 5000.00"
              class="w-full bg-surface-800 border border-surface-600 text-slate-100 text-sm
                     rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-petroleum-500
                     placeholder-slate-600">
          </div>
        </div>

        <!-- Erro -->
        @if (erro()) {
          <p class="mt-3 text-sm text-danger-400">{{ erro() }}</p>
        }

        <!-- Ações -->
        <div class="flex justify-end gap-3 mt-6">
          <button (click)="fechar()"
            class="px-4 py-2 text-sm text-slate-400 hover:text-slate-200 transition-colors">
            Cancelar
          </button>
          <button (click)="salvar()" [disabled]="salvando()"
            class="px-5 py-2 text-sm font-semibold bg-petroleum-700 hover:bg-petroleum-600
                   text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            {{ salvando() ? 'Salvando...' : 'Salvar Meta' }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class DefinirMetaDialogComponent {
  private metaService = inject(MetaService);

  readonly mecanico = input.required<UserResponse>();
  readonly metaSalva = output<void>();
  readonly cancelado = output<void>();

  mes = new Date().getMonth() + 1;
  ano = new Date().getFullYear();
  valorMeta: number | null = null;

  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);

  readonly meses = [
    { valor: 1, label: 'Janeiro' }, { valor: 2, label: 'Fevereiro' },
    { valor: 3, label: 'Março' },   { valor: 4, label: 'Abril' },
    { valor: 5, label: 'Maio' },    { valor: 6, label: 'Junho' },
    { valor: 7, label: 'Julho' },   { valor: 8, label: 'Agosto' },
    { valor: 9, label: 'Setembro' },{ valor: 10, label: 'Outubro' },
    { valor: 11, label: 'Novembro' },{ valor: 12, label: 'Dezembro' },
  ];

  readonly anos = Array.from({ length: 3 }, (_, i) => new Date().getFullYear() + 1 - i);

  salvar() {
    if (!this.valorMeta || this.valorMeta <= 0) {
      this.erro.set('Informe um valor de meta válido maior que zero.');
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);

    this.metaService.definirMeta({
      mecanicoId: this.mecanico().id,
      mes: this.mes,
      ano: this.ano,
      valorMeta: this.valorMeta,
    }).subscribe({
      next: () => { this.salvando.set(false); this.metaSalva.emit(); },
      error: () => {
        this.erro.set('Erro ao salvar meta. Verifique os dados e tente novamente.');
        this.salvando.set(false);
      },
    });
  }

  fechar() { this.cancelado.emit(); }
}
