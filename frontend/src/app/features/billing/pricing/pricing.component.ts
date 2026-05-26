import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { SubscriptionService } from '@core/services/subscription.service';
import { SubscriptionPlan } from '@core/models/subscription.model';

interface Plano {
  id: SubscriptionPlan;
  nome: string;
  preco: string;
  descricao: string;
  recursos: string[];
  destaque: boolean;
}

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-wrapper">
      <div class="page-header">
        <div>
          <h1 class="page-title">Escolha seu Plano</h1>
          <p class="page-subtitle">Comece gratuitamente e escale conforme o crescimento da sua oficina</p>
        </div>
      </div>

      @if (erro()) {
        <div class="mb-6 alert-danger flex items-center gap-2">
          <span>{{ erro() }}</span>
          <button (click)="erro.set(null)" class="ml-auto text-danger-400 hover:text-white">✕</button>
        </div>
      }

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
        @for (plano of planos; track plano.id) {
          <div class="card relative flex flex-col"
               [class.ring-2]="plano.destaque"
               [class.ring-petroleum-500]="plano.destaque">

            @if (plano.destaque) {
              <div class="absolute -top-3 left-1/2 -translate-x-1/2">
                <span class="bg-petroleum-600 text-white text-xs font-bold px-3 py-1 rounded-full">
                  Mais popular
                </span>
              </div>
            }

            <div class="mb-6">
              <h2 class="text-lg font-bold text-white">{{ plano.nome }}</h2>
              <p class="text-sm text-slate-500 mt-1">{{ plano.descricao }}</p>
              <div class="mt-4 flex items-baseline gap-1">
                <span class="text-3xl font-extrabold text-white">{{ plano.preco }}</span>
                <span class="text-slate-500 text-sm">/mês</span>
              </div>
            </div>

            <ul class="flex-1 space-y-2.5 mb-8">
              @for (recurso of plano.recursos; track recurso) {
                <li class="flex items-center gap-2 text-sm text-slate-300">
                  <svg viewBox="0 0 24 24" class="w-4 h-4 text-success-400 flex-shrink-0 fill-current">
                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                  </svg>
                  {{ recurso }}
                </li>
              }
            </ul>

            <button
              (click)="assinar(plano.id)"
              [disabled]="carregando() === plano.id"
              class="w-full py-2.5 rounded-lg text-sm font-semibold transition-all duration-150 disabled:opacity-50"
              [class.bg-petroleum-600]="plano.destaque"
              [class.hover:bg-petroleum-500]="plano.destaque"
              [class.text-white]="plano.destaque"
              [class.bg-surface-700]="!plano.destaque"
              [class.hover:bg-surface-600]="!plano.destaque"
              [class.text-slate-200]="!plano.destaque">
              @if (carregando() === plano.id) {
                <span class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></span>
                Redirecionando...
              } @else {
                Assinar {{ plano.nome }}
              }
            </button>
          </div>
        }
      </div>

      <p class="text-center text-xs text-slate-600 mt-8">
        Pagamento seguro via Stripe · Cancele a qualquer momento · Sem multa de cancelamento
      </p>
    </div>
  `,
})
export class PricingComponent {
  private sub    = inject(SubscriptionService);
  private router = inject(Router);

  readonly carregando = signal<SubscriptionPlan | null>(null);
  readonly erro       = signal<string | null>(null);

  readonly planos: Plano[] = [
    {
      id: 'STARTER',
      nome: 'Starter',
      preco: 'R$ 89',
      descricao: 'Ideal para oficinas de até 2 mecânicos',
      destaque: false,
      recursos: [
        'Até 50 ordens de serviço/mês',
        'Gestão de veículos',
        'Cofre de documentos (5 GB)',
        'Rastreio público de OS',
        'Suporte via e-mail',
      ],
    },
    {
      id: 'PROFESSIONAL',
      nome: 'Profissional',
      preco: 'R$ 179',
      descricao: 'Para oficinas em crescimento com múltiplos mecânicos',
      destaque: true,
      recursos: [
        'Ordens de serviço ilimitadas',
        'Todos os recursos do Starter',
        'Módulo de Metas por Mecânico',
        'Módulo Financeiro',
        'Relatórios e analytics',
        'Cofre de documentos (50 GB)',
        'Suporte prioritário',
      ],
    },
    {
      id: 'ENTERPRISE',
      nome: 'Enterprise',
      preco: 'R$ 349',
      descricao: 'Para redes de oficinas e alto volume',
      destaque: false,
      recursos: [
        'Tudo do Profissional',
        'Multi-unidade (multi-tenant)',
        'Integração com DETRAN',
        'API pública',
        'Cofre ilimitado',
        'SLA garantido 99,9%',
        'Gerente de conta dedicado',
      ],
    },
  ];

  assinar(plano: SubscriptionPlan): void {
    this.carregando.set(plano);
    this.erro.set(null);

    this.sub.criarCheckout(plano).subscribe({
      next: res => {
        this.sub.invalidateStatus();
        window.location.href = res.checkoutUrl;
      },
      error: () => {
        this.carregando.set(null);
        this.erro.set('Não foi possível iniciar o checkout. Tente novamente.');
      },
    });
  }
}
