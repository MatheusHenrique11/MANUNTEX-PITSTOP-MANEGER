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
        <h1 class="text-2xl font-bold text-gray-900">⚙️ Controle de Módulos</h1>
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
                    <a
                      href="/admin/toggles"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="text-xs text-brand-600 hover:text-brand-800 font-medium underline">
                      Gerenciar no Togglz →
                    </a>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <!-- Aviso -->
        <div class="mt-6 p-4 bg-amber-50 border border-amber-200 rounded-lg">
          <p class="text-sm text-amber-800">
            <strong>Painel Togglz:</strong> Para ativar ou desativar módulos, acesse
            <a href="/admin/toggles" target="_blank" class="underline font-medium">
              /admin/toggles
            </a>.
            Apenas administradores têm acesso. As mudanças refletem imediatamente via cache invalidation.
          </p>
        </div>
      }

    </div>
  `,
})
export class FeatureFlagsComponent implements OnInit {
  private featureFlagService = inject(FeatureFlagService);

  readonly loading = signal(true);
  readonly flags = signal<FeatureFlagRow[]>([]);

  ngOnInit() {
    this.featureFlagService.load().subscribe({
      next: (flagsMap: FeatureFlagsMap) => {
        const rows: FeatureFlagRow[] = (Object.entries(flagsMap) as [FeatureName, { active: boolean; label: string }][])
          .map(([name, data]) => ({ name, label: data.label, active: data.active }));
        this.flags.set(rows);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
