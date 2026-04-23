import { Component, inject, signal, OnInit, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '@core/services/auth.service';
import { VeiculoService } from '@core/services/veiculo.service';
import { FeatureFlagService } from '@core/services/feature-flag.service';
import { FeatureName, DISABLED_MODULE_PARAM } from '@core/models/feature-flag.model';

interface StatCard {
  label: string;
  value: string | number;
  sub: string;
  iconBg: string;
  iconColor: string;
  icon: string;
}

interface QuickAction {
  label: string;
  sub: string;
  path: string;
  icon: string;
  feature?: FeatureName;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-wrapper">

      @if (disabledModuleAlert()) {
        <div class="mb-4 alert-warning">
          <svg viewBox="0 0 24 24" class="w-4 h-4 mt-0.5 flex-shrink-0 fill-current text-safety-400">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
          </svg>
          <span>
            O módulo <strong>{{ disabledModuleAlert() }}</strong> está desativado neste tenant.
            Entre em contato com o administrador para habilitá-lo.
          </span>
          <button (click)="dismissModuleAlert()"
                  class="ml-auto flex-shrink-0 text-safety-400 hover:text-white transition-colors">
            <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
      }

      <div class="page-header">
        <div>
          <h1 class="page-title">Dashboard</h1>
          <p class="page-subtitle">{{ greeting() }}, {{ firstName() }}</p>
        </div>
        <div class="flex items-center gap-2 text-xs text-slate-500 bg-surface-800
                    border border-surface-700 rounded-lg px-3 py-2">
          <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current text-petroleum-500">
            <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z"/>
          </svg>
          {{ today }}
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
        @for (card of stats(); track card.label) {
          <div class="stat-card">
            <div class="stat-icon" [class]="card.iconBg">
              <span [class]="card.iconColor">{{ card.icon }}</span>
            </div>
            <div class="min-w-0">
              <p class="text-xs text-slate-500 font-medium uppercase tracking-wide">{{ card.label }}</p>
              <p class="text-2xl font-extrabold text-white mt-0.5 leading-none">
                @if (loadingStats()) {
                  <span class="inline-block w-12 h-6 bg-surface-700 rounded animate-pulse"></span>
                } @else {
                  {{ card.value }}
                }
              </p>
              <p class="text-xs text-slate-500 mt-1">{{ card.sub }}</p>
            </div>
          </div>
        }
      </div>

      <div class="mb-8">
        <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Ações Rápidas</h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
          @for (action of visibleQuickActions(); track action.label) {
            <a [routerLink]="action.path"
               class="card-hover flex flex-col items-center gap-3 py-6 text-center group">
              <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl
                          bg-surface-700 group-hover:bg-petroleum-700/30 transition-colors duration-200">
                {{ action.icon }}
              </div>
              <div>
                <p class="text-sm font-semibold text-slate-200 group-hover:text-white">{{ action.label }}</p>
                <p class="text-xs text-slate-500 mt-0.5">{{ action.sub }}</p>
              </div>
            </a>
          }
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">

        <div class="card">
          <h3 class="text-sm font-semibold text-slate-300 mb-4 flex items-center gap-2">
            <span class="w-2 h-2 bg-safety-500 rounded-full"></span>
            Legenda — Status de OS
          </h3>
          <div class="space-y-2.5">
            @for (s of statusLegend; track s.label) {
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2.5">
                  <span class="w-2 h-2 rounded-full flex-shrink-0" [class]="s.dot"></span>
                  <span class="text-sm text-slate-300">{{ s.label }}</span>
                </div>
                <span class="badge" [class]="s.badge">{{ s.label }}</span>
              </div>
            }
          </div>
        </div>

        <div class="card">
          <h3 class="text-sm font-semibold text-slate-300 mb-4 flex items-center gap-2">
            <span class="w-2 h-2 bg-petroleum-500 rounded-full"></span>
            Informações do Sistema
          </h3>
          <div class="space-y-3">
            @for (info of systemInfo; track info.key) {
              <div class="flex items-center justify-between py-1 border-b border-surface-700 last:border-0">
                <span class="text-xs text-slate-500">{{ info.key }}</span>
                <span class="text-xs font-mono text-slate-300">{{ info.value }}</span>
              </div>
            }
          </div>
        </div>
      </div>

    </div>
  `,
})
export class DashboardComponent implements OnInit {
  private auth = inject(AuthService);
  private veiculoService = inject(VeiculoService);
  private featureFlags = inject(FeatureFlagService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  readonly loadingStats = signal(true);
  readonly totalVeiculos = signal(0);

  private readonly _disabledModuleAlert = signal<string | null>(null);
  readonly disabledModuleAlert = this._disabledModuleAlert.asReadonly();

  readonly today = new Date().toLocaleDateString('pt-BR', {
    weekday: 'long', day: '2-digit', month: 'long', year: 'numeric',
  });

  readonly greeting = computed(() => {
    const h = new Date().getHours();
    if (h < 12) return 'Bom dia';
    if (h < 18) return 'Boa tarde';
    return 'Boa noite';
  });

  readonly firstName = computed(() => {
    const email = this.auth.email?.() ?? '';
    return email.split('@')[0] || 'usuário';
  });

  readonly stats = computed<StatCard[]>(() => [
    {
      label: 'Veículos',
      value: this.totalVeiculos(),
      sub: 'cadastrados no sistema',
      iconBg: 'bg-petroleum-700/20 border border-petroleum-700/30',
      iconColor: 'text-petroleum-400 text-xl',
      icon: '🚗',
    },
    {
      label: 'OS Abertas',
      value: '—',
      sub: 'ordens em andamento',
      iconBg: 'bg-safety-600/20 border border-safety-600/30',
      iconColor: 'text-safety-400 text-xl',
      icon: '🔩',
    },
    {
      label: 'Documentos',
      value: '—',
      sub: 'armazenados com criptografia',
      iconBg: 'bg-surface-700 border border-surface-600',
      iconColor: 'text-slate-300 text-xl',
      icon: '📄',
    },
    {
      label: 'OS Concluídas',
      value: '—',
      sub: 'neste mês',
      iconBg: 'bg-success-600/20 border border-success-600/30',
      iconColor: 'text-success-400 text-xl',
      icon: '✅',
    },
  ]);

  private readonly ALL_QUICK_ACTIONS: QuickAction[] = [
    { label: 'Nova OS',      sub: 'Abrir ordem de serviço', path: '/manutencoes',       icon: '🔩', feature: 'MAINTENANCE_MODULE' },
    { label: 'Novo Veículo', sub: 'Cadastrar veículo',      path: '/veiculos/novo',     icon: '🚗', feature: 'VEHICLE_MANAGEMENT' },
    { label: 'Upload Doc.',  sub: 'Enviar documento',        path: '/documentos/upload', icon: '📤', feature: 'DOCUMENT_VAULT' },
    { label: 'Relatórios',  sub: 'Ver análises',            path: '/relatorios',        icon: '📊', feature: 'ANALYTICS_DASHBOARD' },
  ];

  readonly visibleQuickActions = computed<QuickAction[]>(() => {
    if (this.auth.isAdmin()) return this.ALL_QUICK_ACTIONS;
    return this.ALL_QUICK_ACTIONS.filter(
      a => !a.feature || this.featureFlags.isActive(a.feature)
    );
  });

  readonly statusLegend = [
    { label: 'Aguardando',   dot: 'bg-slate-400',   badge: 'badge-inactive' },
    { label: 'Em andamento', dot: 'bg-safety-500',  badge: 'badge-warning' },
    { label: 'Concluída',    dot: 'bg-success-500', badge: 'badge-active' },
    { label: 'Cancelada',    dot: 'bg-danger-500',  badge: 'badge-danger' },
  ];

  readonly systemInfo = [
    { key: 'Versão',        value: 'v1.0.0' },
    { key: 'Backend',       value: 'Spring Boot 3.3' },
    { key: 'Banco',         value: 'PostgreSQL 16' },
    { key: 'Segurança',     value: 'JWT + AES-256-GCM' },
    { key: 'Armazenamento', value: 'S3 / R2 Cloudflare' },
  ];

  constructor() {
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const feature = params[DISABLED_MODULE_PARAM] as FeatureName | undefined;
        if (feature) {
          this._disabledModuleAlert.set(this.featureFlags.labelFor(feature));
          this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { [DISABLED_MODULE_PARAM]: null },
            queryParamsHandling: 'merge',
            replaceUrl: true,
          });
        }
      });
  }

  dismissModuleAlert(): void {
    this._disabledModuleAlert.set(null);
  }

  ngOnInit() {
    this.veiculoService.listar(0, 1).subscribe({
      next: p => {
        this.totalVeiculos.set(p.totalElements);
        this.loadingStats.set(false);
      },
      error: () => this.loadingStats.set(false),
    });
  }
}
