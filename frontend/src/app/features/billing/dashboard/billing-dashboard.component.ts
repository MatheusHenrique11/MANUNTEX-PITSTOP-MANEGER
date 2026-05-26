import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { SubscriptionService } from '@core/services/subscription.service';
import {
  AssinaturaResponse,
  FaturaNfeResponse,
  PLAN_LABELS,
  STATUS_LABELS,
  SubscriptionStatus,
} from '@core/models/subscription.model';

@Component({
  selector: 'app-billing-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-wrapper">
      <div class="page-header">
        <div>
          <h1 class="page-title">Faturamento</h1>
          <p class="page-subtitle">Gerencie sua assinatura e notas fiscais</p>
        </div>
        <a routerLink="/billing/pricing"
           class="btn-primary text-sm">
          Alterar plano
        </a>
      </div>

      @if (successAlert()) {
        <div class="mb-6 alert-success flex items-center gap-2">
          <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current flex-shrink-0"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          <span>Pagamento confirmado! Sua assinatura foi ativada.</span>
          <button (click)="successAlert.set(false)" class="ml-auto">✕</button>
        </div>
      }

      <!-- Cartão de assinatura -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-8">
        <div class="card lg:col-span-2">
          <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
            Assinatura atual
          </h2>

          @if (carregando()) {
            <div class="space-y-3">
              @for (i of [1,2,3]; track i) {
                <div class="h-4 bg-surface-700 rounded animate-pulse w-3/4"></div>
              }
            </div>
          } @else if (assinatura()) {
            <div class="space-y-4">
              <div class="flex items-center justify-between">
                <span class="text-sm text-slate-500">Plano</span>
                <span class="text-sm font-semibold text-white">
                  {{ planLabel(assinatura()!.plano) }}
                </span>
              </div>
              <div class="flex items-center justify-between border-t border-surface-700 pt-4">
                <span class="text-sm text-slate-500">Status</span>
                <span class="badge" [class]="statusBadgeClass(assinatura()!.status)">
                  {{ statusLabel(assinatura()!.status) }}
                </span>
              </div>
              @if (assinatura()!.currentPeriodEnd) {
                <div class="flex items-center justify-between border-t border-surface-700 pt-4">
                  <span class="text-sm text-slate-500">Próxima renovação</span>
                  <span class="text-sm text-slate-200">
                    {{ assinatura()!.currentPeriodEnd | date:'dd/MM/yyyy' : 'UTC' }}
                  </span>
                </div>
              }
              @if (assinatura()!.trialEnd) {
                <div class="flex items-center justify-between border-t border-surface-700 pt-4">
                  <span class="text-sm text-slate-500">Trial expira em</span>
                  <span class="text-sm text-safety-400 font-medium">
                    {{ assinatura()!.trialEnd | date:'dd/MM/yyyy' : 'UTC' }}
                  </span>
                </div>
              }
            </div>
          } @else {
            <div class="text-center py-8">
              <p class="text-sm text-slate-500 mb-4">Nenhuma assinatura encontrada.</p>
              <a routerLink="/billing/pricing" class="btn-primary text-sm">Ver planos</a>
            </div>
          }
        </div>

        <!-- Ações rápidas -->
        <div class="card flex flex-col gap-3">
          <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-2">Ações</h2>
          <a routerLink="/billing/pricing"
             class="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-surface-700
                    hover:bg-surface-600 text-sm text-slate-200 transition-colors">
            <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current text-petroleum-400 flex-shrink-0">
              <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
            </svg>
            Alterar plano
          </a>
          @if (assinatura()?.status === 'ACTIVE' || assinatura()?.status === 'TRIAL') {
            <button (click)="cancelar()"
                    [disabled]="cancelando()"
                    class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm
                           text-danger-400 hover:bg-danger-600/10 hover:border-danger-600/20
                           border border-transparent transition-colors disabled:opacity-50">
              <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current flex-shrink-0">
                <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
              {{ cancelando() ? 'Cancelando...' : 'Cancelar assinatura' }}
            </button>
          }
        </div>
      </div>

      <!-- Histórico de faturas / NFS-e -->
      <div class="card">
        <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
          Faturas e Notas Fiscais
        </h2>

        @if (carregandoFaturas()) {
          <div class="space-y-3">
            @for (i of [1,2,3]; track i) {
              <div class="h-10 bg-surface-700 rounded animate-pulse"></div>
            }
          </div>
        } @else if (faturas().length === 0) {
          <div class="text-center py-10">
            <span class="text-3xl">📄</span>
            <p class="text-sm text-slate-500 mt-3">Nenhuma fatura emitida ainda.</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-surface-700 text-left">
                  <th class="pb-3 text-xs text-slate-500 font-medium">Data</th>
                  <th class="pb-3 text-xs text-slate-500 font-medium">NFS-e</th>
                  <th class="pb-3 text-xs text-slate-500 font-medium">Status</th>
                  <th class="pb-3 text-xs text-slate-500 font-medium text-right">Valor</th>
                  <th class="pb-3 text-xs text-slate-500 font-medium text-center">Downloads</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-surface-700">
                @for (fatura of faturas(); track fatura.id) {
                  <tr>
                    <td class="py-3 text-slate-300">
                      {{ fatura.issueDate | date:'dd/MM/yyyy' : 'UTC' }}
                    </td>
                    <td class="py-3 text-slate-400 font-mono text-xs">
                      {{ fatura.nfeId ?? '—' }}
                    </td>
                    <td class="py-3">
                      <span class="badge" [class]="nfeBadgeClass(fatura.nfeStatus)">
                        {{ fatura.nfeStatus ?? 'Processando' }}
                      </span>
                    </td>
                    <td class="py-3 text-slate-200 text-right font-semibold">
                      {{ fatura.valor | currency:'BRL':'symbol':'1.2-2' }}
                    </td>
                    <td class="py-3 text-center">
                      <div class="flex items-center justify-center gap-2">
                        @if (fatura.pdfUrl) {
                          <a [href]="fatura.pdfUrl" target="_blank" rel="noopener"
                             class="text-petroleum-400 hover:text-petroleum-300 text-xs
                                    flex items-center gap-1 transition-colors">
                            <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current">
                              <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
                            </svg>
                            PDF
                          </a>
                        }
                        @if (fatura.xmlUrl) {
                          <a [href]="fatura.xmlUrl" target="_blank" rel="noopener"
                             class="text-slate-400 hover:text-slate-200 text-xs
                                    flex items-center gap-1 transition-colors">
                            <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current">
                              <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
                            </svg>
                            XML
                          </a>
                        }
                        @if (!fatura.pdfUrl && !fatura.xmlUrl) {
                          <span class="text-xs text-slate-600">—</span>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
})
export class BillingDashboardComponent implements OnInit {
  private sub   = inject(SubscriptionService);
  private route = inject(ActivatedRoute);

  readonly carregando        = signal(true);
  readonly carregandoFaturas = signal(true);
  readonly cancelando        = signal(false);
  readonly successAlert      = signal(false);
  readonly assinatura        = signal<AssinaturaResponse | null>(null);
  readonly faturas           = signal<FaturaNfeResponse[]>([]);

  ngOnInit() {
    // Detecta retorno do Stripe Checkout com sucesso
    this.route.queryParams.subscribe(params => {
      if (params['success'] === 'true') {
        this.successAlert.set(true);
        this.sub.invalidateStatus();
      }
    });

    this.sub.getAssinatura().subscribe({
      next:  a  => { this.assinatura.set(a); this.carregando.set(false); },
      error: () => this.carregando.set(false),
    });

    this.sub.getFaturas().subscribe({
      next:  f  => { this.faturas.set(f); this.carregandoFaturas.set(false); },
      error: () => this.carregandoFaturas.set(false),
    });
  }

  cancelar() {
    if (!confirm('Tem certeza? A assinatura será cancelada ao final do período atual.')) return;
    this.cancelando.set(true);
    this.sub.cancelarAssinatura().subscribe({
      next: () => {
        this.sub.invalidateStatus();
        this.cancelando.set(false);
        this.sub.getAssinatura().subscribe(a => this.assinatura.set(a));
      },
      error: () => this.cancelando.set(false),
    });
  }

  planLabel(plano: string): string {
    return PLAN_LABELS[plano as keyof typeof PLAN_LABELS] ?? plano;
  }

  statusLabel(status: SubscriptionStatus): string {
    return STATUS_LABELS[status] ?? status;
  }

  statusBadgeClass(status: SubscriptionStatus): string {
    const map: Record<SubscriptionStatus, string> = {
      TRIAL:     'badge-warning',
      ACTIVE:    'badge-active',
      CANCELED:  'badge-inactive',
      PAST_DUE:  'badge-danger',
      SUSPENDED: 'badge-danger',
    };
    return map[status] ?? 'badge-inactive';
  }

  nfeBadgeClass(status?: string): string {
    if (!status) return 'badge-inactive';
    if (status === 'autorizado') return 'badge-active';
    if (status === 'erro_emissao') return 'badge-danger';
    return 'badge-warning';
  }
}
