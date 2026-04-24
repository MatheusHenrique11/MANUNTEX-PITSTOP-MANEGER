import { Component, inject, computed, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '@core/services/auth.service';
import { FeatureFlagService } from '@core/services/feature-flag.service';
import { FeatureName } from '@core/models/feature-flag.model';
import { UserRole, ROLE_LABELS } from '@core/models/user.model';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  feature?: FeatureName;
  adminOnly?: boolean;
  /** Roles que podem ver este item. Vazio = todos autenticados. */
  roles?: UserRole[];
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <div class="flex h-screen bg-surface-950 overflow-hidden">

      <!-- Mobile overlay -->
      @if (sidebarOpen()) {
        <div class="fixed inset-0 z-20 bg-black/60 lg:hidden" (click)="sidebarOpen.set(false)"></div>
      }

      <!-- Sidebar -->
      <aside
        class="fixed lg:relative z-30 lg:z-auto inset-y-0 left-0 flex flex-col w-64 bg-surface-900
               border-r border-surface-700 shadow-xl transition-transform duration-300 ease-in-out
               lg:translate-x-0"
        [class.-translate-x-full]="!sidebarOpen()"
        [class.translate-x-0]="sidebarOpen()">

        <!-- Logo -->
        <div class="px-5 py-5 border-b border-surface-700 flex items-center gap-3">
          <div class="w-9 h-9 bg-petroleum-700 rounded-lg flex items-center justify-center flex-shrink-0 shadow-glow-petroleum">
            <svg viewBox="0 0 24 24" class="w-5 h-5 text-white fill-current">
              <path d="M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z"/>
            </svg>
          </div>
          <div>
            <p class="text-sm font-bold text-white leading-tight">PitStop</p>
            <p class="text-xs text-slate-500 leading-tight">Manutex Manager</p>
          </div>
        </div>

        <!-- Nav section label -->
        <div class="px-5 pt-5 pb-2">
          <p class="text-xs font-semibold text-slate-600 uppercase tracking-widest">Navegação</p>
        </div>

        <!-- Nav items -->
        <nav class="flex-1 px-3 overflow-y-auto space-y-0.5">
          @for (item of visibleNavItems(); track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="bg-petroleum-700/20 text-petroleum-400 border-l-2 border-petroleum-500"
              [routerLinkActiveOptions]="{ exact: item.path === '/' }"
              class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-slate-400
                     hover:bg-surface-700 hover:text-slate-100 transition-all duration-150
                     text-sm font-medium border-l-2 border-transparent"
              (click)="sidebarOpen.set(false)">
              <span class="text-base w-5 text-center">{{ item.icon }}</span>
              <span>{{ item.label }}</span>
            </a>
          }
        </nav>

        <!-- Bottom: user info + logout -->
        <div class="px-4 py-4 border-t border-surface-700 space-y-3">
          <div class="flex items-center gap-3 px-2">
            <div class="w-8 h-8 bg-safety-600/20 border border-safety-600/40 rounded-full
                        flex items-center justify-center flex-shrink-0">
              <span class="text-xs font-bold text-safety-400">
                {{ userInitial() }}
              </span>
            </div>
            <div class="min-w-0">
              <p class="text-xs font-semibold text-slate-200 truncate">{{ userEmail() }}</p>
              <p class="text-xs text-slate-500">{{ roleLabel() }}</p>
            </div>
          </div>
          <button
            (click)="logout()"
            class="w-full text-left text-sm text-slate-500 hover:text-danger-400
                   flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-danger-600/10
                   border border-transparent hover:border-danger-600/20 transition-all duration-150">
            <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current flex-shrink-0">
              <path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>
            </svg>
            Sair da conta
          </button>
        </div>
      </aside>

      <!-- Main area -->
      <div class="flex-1 flex flex-col min-w-0 overflow-hidden">

        <!-- Mobile topbar -->
        <header class="lg:hidden flex items-center justify-between px-4 py-3
                       bg-surface-900 border-b border-surface-700">
          <button
            (click)="sidebarOpen.set(!sidebarOpen())"
            class="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-surface-700 transition-colors">
            <svg viewBox="0 0 24 24" class="w-5 h-5 fill-current">
              <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/>
            </svg>
          </button>
          <span class="text-sm font-bold text-white">PitStop Manager</span>
          <div class="w-8"></div>
        </header>

        <!-- Content -->
        <main class="flex-1 overflow-auto bg-surface-950">
          <router-outlet />
        </main>
      </div>

    </div>
  `,
})
export class ShellComponent {
  private auth = inject(AuthService);
  private featureFlags = inject(FeatureFlagService);
  private router = inject(Router);

  readonly sidebarOpen = signal(false);

  private readonly NAV_ITEMS: NavItem[] = [
    { label: 'Dashboard',   path: '/dashboard',         icon: '▦' },
    { label: 'Veículos',    path: '/veiculos',          icon: '🚗', feature: 'VEHICLE_MANAGEMENT' },
    { label: 'Manutenções', path: '/manutencoes',       icon: '🔩', feature: 'MAINTENANCE_MODULE' },
    { label: 'Documentos',  path: '/documentos/upload', icon: '📄', feature: 'DOCUMENT_VAULT' },
    // Metas — gerente/admin vê /metas/gerente; mecânico vê /metas/mecanico
    {
      label: 'Metas',
      path: '/metas/gerente',
      icon: '🎯',
      feature: 'GOALS_MODULE',
      roles: ['ROLE_GERENTE', 'ROLE_ADMIN'],
    },
    {
      label: 'Minhas Metas',
      path: '/metas/mecanico',
      icon: '🎯',
      feature: 'GOALS_MODULE',
      roles: ['ROLE_MECANICO'],
    },
    { label: 'Financeiro',  path: '/financeiro',        icon: '💰', feature: 'FINANCIAL_MODULE' },
    { label: 'Relatórios',  path: '/relatorios',        icon: '📊', feature: 'ANALYTICS_DASHBOARD' },
    { label: 'Usuários',    path: '/admin/usuarios',    icon: '👥', adminOnly: true },
    { label: 'Módulos',     path: '/admin/modulos',     icon: '⚙',  adminOnly: true },
  ];

  readonly visibleNavItems = computed(() => {
    const role = this.auth.role() as UserRole | null;
    return this.NAV_ITEMS.filter(item => {
      if (item.adminOnly && !this.auth.isAdmin()) return false;
      if (item.roles && role && !item.roles.includes(role)) return false;
      if (item.feature && !this.auth.isAdmin()) return this.featureFlags.isActive(item.feature);
      return true;
    });
  });

  readonly roleLabel = computed(() =>
    ROLE_LABELS[this.auth.role() as UserRole] ?? 'Usuário'
  );

  readonly userEmail = computed(() => this.auth.email?.() ?? '');
  readonly userInitial = computed(() => (this.auth.email?.() ?? 'U')[0].toUpperCase());

  logout() { this.auth.logout(); }
}
