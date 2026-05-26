import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-relatorios',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-wrapper">
      <div class="page-header">
        <div>
          <h1 class="page-title">Relatórios</h1>
          <p class="page-subtitle">Analytics e relatórios gerenciais</p>
        </div>
      </div>

      <div class="card flex flex-col items-center justify-center py-16 text-center">
        <div class="w-16 h-16 bg-surface-700 rounded-xl flex items-center justify-center mb-4">
          <span class="text-3xl">📊</span>
        </div>
        <h2 class="text-lg font-semibold text-slate-200 mb-2">Módulo em Desenvolvimento</h2>
        <p class="text-sm text-slate-500 max-w-sm">
          O módulo de Relatórios está sendo implementado e estará disponível em breve.
        </p>
      </div>
    </div>
  `,
})
export class RelatoriosComponent {}
