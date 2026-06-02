import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FeatureFlagService } from '@core/services/feature-flag.service';
import { FeatureFlagsMap, FeatureName } from '@core/models/feature-flag.model';

interface FeatureFlagRow {
  name: FeatureName;
  label: string;
  active: boolean;
}

@Component({
  selector: 'app-feature-flags',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-8 max-w-4xl mx-auto">

      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900">Controle de Módulos</h1>
        <p class="mt-1 text-sm text-gray-500">
          Ative ou desative funcionalidades em tempo real sem necessidade de deploy.
          As mudanças são aplicadas imediatamente para todos os usuários.
        </p>
      </div>

      @if (loading()) {
        <div class="card text-center py-12 text-gray-400">Carregando módulos...</div>
      } @else {
        <div class="card p-0 overflow-hidden">
          <table class="w-full">
            <thead class="bg-gray-50 border-b border-gray-200">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  Módulo
                </th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  Identificador
                </th>
                <th class="px-6 py-3 text-center text-xs font-semibold text-gray-500 uppercase tracking-wider w-28">
                  Status
                </th>
                <th class="px-6 py-3 text-center text-xs font-semibold text-gray-500 uppercase tracking-wider w-36">
                  Ação
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              @for (flag of flags(); track flag.name) {
                <tr class="hover:bg-gray-50 transition-colors">
                  <td class="px-6 py-4">
                    <p class="text-sm font-medium text-gray-900">{{ flag.label }}</p>
                  </td>
                  <td class="px-6 py-4">
                    <code class="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                      {{ flag.name }}
                    </code>
                  </td>
                  <td class="px-6 py-4 text-center">
                    @if (flag.active) {
                      <span class="badge-active">Ativo</span>
                    } @else {
                      <span class="badge-inactive">Inativo</span>
                    }
                  </td>
                  <td class="px-6 py-4 text-center">
                    <button
                      (click)="toggle(flag)"
                      [disabled]="toggling() === flag.name"
                      class="text-xs font-medium px-3 py-1.5 rounded border transition-colors disabled:opacity-50"
                      [class]="flag.active
                        ? 'border-red-300 text-red-600 hover:bg-red-50'
                        : 'border-green-300 text-green-600 hover:bg-green-50'">
                      @if (toggling() === flag.name) {
                        Salvando...
                      } @else {
                        {{ flag.active ? 'Desativar' : 'Ativar' }}
                      }
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

    </div>
  `,
})
export class FeatureFlagsComponent implements OnInit {
  private featureFlagService = inject(FeatureFlagService);

  readonly loading = signal(true);
  readonly flags = signal<FeatureFlagRow[]>([]);
  readonly toggling = signal<FeatureName | null>(null);

  ngOnInit() {
    this.featureFlagService.load().subscribe({
      next: (flagsMap: FeatureFlagsMap) => {
        this.flags.set(this.toRows(flagsMap));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggle(flag: FeatureFlagRow) {
    this.toggling.set(flag.name);
    this.featureFlagService.toggle(flag.name).subscribe({
      next: (result) => {
        this.flags.update(rows =>
          rows.map(r => r.name === flag.name ? { ...r, active: result.active } : r)
        );
        this.toggling.set(null);
      },
      error: () => this.toggling.set(null),
    });
  }

  private toRows(flagsMap: FeatureFlagsMap): FeatureFlagRow[] {
    return (Object.entries(flagsMap) as [FeatureName, { active: boolean; label: string }][])
      .map(([name, data]) => ({ name, label: data.label, active: data.active }));
  }
}
