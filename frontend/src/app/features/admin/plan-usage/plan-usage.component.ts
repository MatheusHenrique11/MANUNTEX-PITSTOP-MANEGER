import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SubscriptionService } from '@core/services/subscription.service';
import {
  FeatureStatus,
  PLAN_LABELS,
  PLAN_PRICES,
  PlanUsageResponse,
  STATUS_LABELS,
  StorageMetric,
  SubscriptionPlan,
  UsageMetric,
} from '@core/models/subscription.model';

@Component({
  selector: 'app-plan-usage',
  standalone: true,
  imports: [CommonModule, RouterLink, DecimalPipe],
  template: `
    <div class="page-wrapper">

      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Uso do Plano</h1>
          <p class="page-subtitle">Recursos consumidos no período atual e estado das funcionalidades</p>
        </div>
        @if (canUpgrade()) {
          <a routerLink="/billing/pricing" class="btn-primary text-sm">
            Fazer upgrade
          </a>
        }
      </div>

      @if (carregando()) {
        <!-- Skeleton -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          @for (i of [1,2,3]; track i) {
            <div class="card space-y-3">
              <div class="h-3 bg-surface-700 rounded animate-pulse w-2/3"></div>
              <div class="h-7 bg-surface-700 rounded animate-pulse w-1/2"></div>
              <div class="h-2 bg-surface-700 rounded-full animate-pulse"></div>
            </div>
          }
        </div>
      } @else if (usage()) {
        <!-- Plano + status -->
        <div class="card mb-6">
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div class="flex items-center gap-4">
              <div class="w-12 h-12 rounded-xl bg-petroleum-700/20 border border-petroleum-700/40
                          flex items-center justify-center flex-shrink-0">
                <svg viewBox="0 0 24 24" class="w-6 h-6 fill-current text-petroleum-400">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                </svg>
              </div>
              <div>
                <p class="text-xs text-slate-500 uppercase tracking-wider">Plano atual</p>
                <p class="text-xl font-bold text-white">{{ planLabel(usage()!.plano) }}</p>
                <p class="text-sm text-slate-400">{{ planPrice(usage()!.plano) }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <span class="badge text-sm px-3 py-1.5" [class]="statusBadgeClass(usage()!.status)">
                {{ statusLabel(usage()!.status) }}
              </span>
              <a routerLink="/billing/dashboard"
                 class="text-xs text-petroleum-400 hover:text-petroleum-300 transition-colors">
                Ver faturas →
              </a>
            </div>
          </div>
        </div>

        <!-- Métricas de uso -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">

          <!-- OS do mês -->
          <div class="card">
            <div class="flex items-start justify-between mb-3">
              <div>
                <p class="text-xs text-slate-500 uppercase tracking-wider">OS no mês</p>
                <p class="text-2xl font-bold text-white mt-1">
                  {{ usage()!.os.used | number }}
                  @if (!usage()!.os.unlimited) {
                    <span class="text-sm font-normal text-slate-500">
                      / {{ usage()!.os.limit | number }}
                    </span>
                  }
                </p>
              </div>
              <span class="text-2xl">🔩</span>
            </div>
            @if (!usage()!.os.unlimited) {
              <div class="space-y-1">
                <div class="h-2 bg-surface-700 rounded-full overflow-hidden">
                  <div class="h-full rounded-full transition-all duration-500"
                       [class]="progressColor(osPercent())"
                       [style.width.%]="osPercent()">
                  </div>
                </div>
                <p class="text-xs text-right" [class]="progressTextColor(osPercent())">
                  {{ osPercent() | number:'1.0-0' }}% utilizado
                </p>
              </div>
            } @else {
              <p class="text-xs text-slate-500 mt-1">Ilimitado</p>
            }
          </div>

          <!-- Mecânicos -->
          <div class="card">
            <div class="flex items-start justify-between mb-3">
              <div>
                <p class="text-xs text-slate-500 uppercase tracking-wider">Mecânicos ativos</p>
                <p class="text-2xl font-bold text-white mt-1">
                  {{ usage()!.mecanicos.used | number }}
                  @if (!usage()!.mecanicos.unlimited) {
                    <span class="text-sm font-normal text-slate-500">
                      / {{ usage()!.mecanicos.limit | number }}
                    </span>
                  }
                </p>
              </div>
              <span class="text-2xl">👥</span>
            </div>
            @if (!usage()!.mecanicos.unlimited) {
              <div class="space-y-1">
                <div class="h-2 bg-surface-700 rounded-full overflow-hidden">
                  <div class="h-full rounded-full transition-all duration-500"
                       [class]="progressColor(mecanicosPercent())"
                       [style.width.%]="mecanicosPercent()">
                  </div>
                </div>
                <p class="text-xs text-right" [class]="progressTextColor(mecanicosPercent())">
                  {{ mecanicosPercent() | number:'1.0-0' }}% utilizado
                </p>
              </div>
            } @else {
              <p class="text-xs text-slate-500 mt-1">Ilimitado</p>
            }
          </div>

          <!-- Storage -->
          <div class="card">
            <div class="flex items-start justify-between mb-3">
              <div>
                <p class="text-xs text-slate-500 uppercase tracking-wider">Armazenamento</p>
                <p class="text-2xl font-bold text-white mt-1">
                  {{ formatBytes(usage()!.storage.usedBytes) }}
                  @if (!usage()!.storage.unlimited) {
                    <span class="text-sm font-normal text-slate-500">
                      / {{ formatBytes(usage()!.storage.limitBytes) }}
                    </span>
                  }
                </p>
              </div>
              <span class="text-2xl">💾</span>
            </div>
            @if (!usage()!.storage.unlimited) {
              <div class="space-y-1">
                <div class="h-2 bg-surface-700 rounded-full overflow-hidden">
                  <div class="h-full rounded-full transition-all duration-500"
                       [class]="progressColor(storagePercent())"
                       [style.width.%]="storagePercent()">
                  </div>
                </div>
                <p class="text-xs text-right" [class]="progressTextColor(storagePercent())">
                  {{ storagePercent() | number:'1.0-0' }}% utilizado
                </p>
              </div>
            } @else {
              <p class="text-xs text-slate-500 mt-1">Ilimitado</p>
            }
          </div>

        </div>

        <!-- Features -->
        <div class="card">
          <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
            Funcionalidades
          </h2>
          <div class="divide-y divide-surface-700">
            @for (f of usage()!.features; track f.name) {
              <div class="flex items-center justify-between py-3">
                <div class="flex items-center gap-3">
                  <span class="w-2 h-2 rounded-full flex-shrink-0"
                        [class]="f.active ? 'bg-green-400' : 'bg-surface-600'">
                  </span>
                  <span class="text-sm text-slate-200">{{ f.label }}</span>
                  @if (!f.includedInPlan) {
                    <span class="text-xs text-safety-500 border border-safety-600/30
                                 bg-safety-600/10 px-1.5 py-0.5 rounded">
                      Plano superior
                    </span>
                  }
                </div>
                <div class="flex items-center gap-3 flex-shrink-0">
                  @if (f.active) {
                    <span class="badge-active text-xs">Ativo</span>
                  } @else {
                    <span class="badge-inactive text-xs">Inativo</span>
                  }
                </div>
              </div>
            }
          </div>
        </div>

        <!-- Banner de upgrade -->
        @if (canUpgrade()) {
          <div class="mt-6 rounded-xl border border-petroleum-600/30 bg-petroleum-900/20 p-5
                      flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <p class="text-sm font-semibold text-slate-200">
                Precisa de mais recursos?
              </p>
              <p class="text-xs text-slate-400 mt-1">
                Faça upgrade para desbloquear OS ilimitadas, mais mecânicos e funcionalidades avançadas.
              </p>
            </div>
            <a routerLink="/billing/pricing"
               class="btn-primary text-sm whitespace-nowrap flex-shrink-0">
              Ver planos
            </a>
          </div>
        }
      } @else {
        <div class="card text-center py-16">
          <p class="text-sm text-slate-500">Não foi possível carregar os dados de uso.</p>
          <button (click)="carregar()" class="btn-primary text-sm mt-4">Tentar novamente</button>
        </div>
      }

    </div>
  `,
})
export class PlanUsageComponent implements OnInit {
  private sub = inject(SubscriptionService);

  readonly carregando = signal(true);
  readonly usage      = signal<PlanUsageResponse | null>(null);

  readonly osPercent = computed(() => {
    const m = this.usage()?.os;
    return m && !m.unlimited && m.limit > 0 ? Math.min((m.used / m.limit) * 100, 100) : 0;
  });

  readonly mecanicosPercent = computed(() => {
    const m = this.usage()?.mecanicos;
    return m && !m.unlimited && m.limit > 0 ? Math.min((m.used / m.limit) * 100, 100) : 0;
  });

  readonly storagePercent = computed(() => {
    const s = this.usage()?.storage;
    return s && !s.unlimited && s.limitBytes > 0
      ? Math.min((s.usedBytes / s.limitBytes) * 100, 100)
      : 0;
  });

  readonly canUpgrade = computed(() =>
    this.usage()?.plano !== 'ENTERPRISE'
  );

  ngOnInit() { this.carregar(); }

  carregar() {
    this.carregando.set(true);
    this.sub.getPlanUsage().subscribe({
      next:  u  => { this.usage.set(u);    this.carregando.set(false); },
      error: () => { this.usage.set(null); this.carregando.set(false); },
    });
  }

  // ── Formatação ──────────────────────────────────────────────────────────────

  planLabel(plano: SubscriptionPlan): string {
    return PLAN_LABELS[plano] ?? plano;
  }

  planPrice(plano: SubscriptionPlan): string {
    return PLAN_PRICES[plano] ?? '';
  }

  statusLabel(status: string): string {
    return STATUS_LABELS[status as keyof typeof STATUS_LABELS] ?? status;
  }

  statusBadgeClass(status: string): string {
    const map: Record<string, string> = {
      TRIAL:     'badge-warning',
      ACTIVE:    'badge-active',
      CANCELED:  'badge-inactive',
      PAST_DUE:  'badge-danger',
      SUSPENDED: 'badge-danger',
    };
    return map[status] ?? 'badge-inactive';
  }

  progressColor(pct: number): string {
    if (pct >= 90) return 'bg-danger-500';
    if (pct >= 70) return 'bg-safety-500';
    return 'bg-petroleum-500';
  }

  progressTextColor(pct: number): string {
    if (pct >= 90) return 'text-danger-400';
    if (pct >= 70) return 'text-safety-400';
    return 'text-slate-500';
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const gb = bytes / (1024 ** 3);
    if (gb >= 1)   return `${gb.toFixed(1)} GB`;
    const mb = bytes / (1024 ** 2);
    if (mb >= 1)   return `${mb.toFixed(1)} MB`;
    const kb = bytes / 1024;
    return `${kb.toFixed(1)} KB`;
  }
}
