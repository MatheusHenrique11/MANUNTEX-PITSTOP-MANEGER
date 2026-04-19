import { Component, inject, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '@core/services/auth.service';
import { FeatureFlagService } from '@core/services/feature-flag.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  feature?: string;
  adminOnly?: boolean;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <div class="flex h-screen bg-gray-50">

      <!-- Sidebar -->
      <aside class="w-64 bg-brand-900 text-white flex flex-col shadow-xl">
        <!-- Logo -->
        <div class="px-6 py-5 border-b border-brand-700">
          <span class="text-xl font-bold tracking-tight">🔧 PitStop Manager</span>
          <p class="text-xs text-brand-100 mt-0.5">Manutex</p>
        </div>

        <!-- Nav -->
        <nav class="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          @for (item of visibleNavItems(); track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="bg-brand-700 text-white"
              class="flex items-center gap-3 px-3 py-2 rounded-lg text-brand-100
                     hover:bg-brand-700 hover:text-white transition-colors text-sm font-medium">
              <span class="text-base">{{ item.icon }}</span>
              {{ item.label }}
            </a>
          }
        </nav>

        <!-- User Info + Logout -->
        <div class="px-4 py-4 border-t border-brand-700">
          <p class="text-xs text-brand-300 mb-2">{{ roleLabel() }}</p>
          <button
            (click)="logout()"
            class="w-full text-left text-sm text-brand-200 hover:text-white
                   flex items-center gap-2 px-2 py-1.5 rounded hover:bg-brand-700 transition-colors">
            <span>↩</span> Sair
          </button>
        </div>
      </aside>

      <!-- Main content -->
      <main class="flex-1 overflow-auto">
        <router-outlet />
      </main>

    </div>
  `,
})
export class ShellComponent {
  private auth = inject(AuthService);
  private featureFlags = inject(FeatureFlagService);
  private router = inject(Router);

  private readonly NAV_ITEMS: NavItem[] = [
    { label: 'Veículos',    path: '/veiculos',       icon: '🚗', feature: 'VEHICLE_MANAGEMENT' },
    { label: 'Documentos',  path: '/documentos/upload', icon: '📄', feature: 'DOCUMENT_VAULT' },
    { label: 'Manutenções', path: '/manutencoes',    icon: '🔩', feature: 'MAINTENANCE_MODULE' },
    { label: 'Financeiro',  path: '/financeiro',     icon: '💰', feature: 'FINANCIAL_MODULE' },
    { label: 'Relatórios',  path: '/relatorios',     icon: '📊', feature: 'ANALYTICS_DASHBOARD' },
    { label: 'Controle de Módulos', path: '/admin/modulos', icon: '⚙️', adminOnly: true },
  ];

  readonly visibleNavItems = computed(() => {
    return this.NAV_ITEMS.filter(item => {
      if (item.adminOnly && !this.auth.isAdmin()) return false;
      if (item.feature) return this.featureFlags.isActive(item.feature as any);
      return true;
    });
  });

  readonly roleLabel = computed(() => {
    const map: Record<string, string> = {
      ROLE_ADMIN: 'Administrador',
      ROLE_GERENTE: 'Gerente',
      ROLE_MECANICO: 'Mecânico',
      ROLE_RECEPCIONISTA: 'Recepcionista',
    };
    return map[this.auth.role() ?? ''] ?? '';
  });

  logout() {
    this.auth.logout();
  }
}
