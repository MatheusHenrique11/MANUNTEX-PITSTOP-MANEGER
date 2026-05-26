import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LgpdService } from '@core/services/lgpd.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-consent',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-surface-950 flex items-center justify-center px-4 py-12">
      <div class="w-full max-w-2xl space-y-6">

        <!-- Header -->
        <div class="text-center">
          <div class="w-12 h-12 bg-petroleum-700 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-glow-petroleum">
            <svg viewBox="0 0 24 24" class="w-6 h-6 text-white fill-current">
              <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 4l5 2.18V11c0 3.5-2.33 6.79-5 7.93-2.67-1.14-5-4.43-5-7.93V7.18L12 5zm-1 5v2h2v-2h-2zm0 4v2h2v-2h-2z"/>
            </svg>
          </div>
          <h1 class="text-2xl font-bold text-white">Seus Dados, Seus Direitos</h1>
          <p class="text-sm text-slate-400 mt-1">
            Para usar o PitStop Manager, precisamos do seu consentimento conforme a
            <span class="text-petroleum-400">Lei Geral de Proteção de Dados (LGPD)</span>.
          </p>
        </div>

        <div class="bg-surface-900 rounded-2xl border border-surface-700 p-8 space-y-6">

          <!-- Política de Privacidade -->
          <div class="space-y-3">
            <h2 class="text-base font-semibold text-white">Política de Privacidade</h2>
            <div class="bg-surface-800 rounded-xl p-4 text-sm text-slate-300 max-h-48 overflow-y-auto leading-relaxed space-y-2">
              <p>O <strong class="text-white">PitStop Manager</strong> (Manutex) coleta e trata dados pessoais para prestação do serviço de gestão de ordens de serviço.</p>
              <p><strong class="text-white">Dados coletados:</strong> nome, e-mail, CPF/CNPJ (clientes), placas e documentos de veículos.</p>
              <p><strong class="text-white">Finalidade:</strong> Gestão operacional da oficina, emissão de NFS-e e cumprimento de obrigações legais.</p>
              <p><strong class="text-white">Base legal:</strong> Execução de contrato (Art. 7, V LGPD) e consentimento (Art. 7, I LGPD).</p>
              <p><strong class="text-white">Retenção:</strong> Dados são mantidos enquanto a conta estiver ativa. Após exclusão, anonimizamos em até 90 dias.</p>
              <p><strong class="text-white">Seus direitos:</strong> Acesso, correção, portabilidade, anonimização e oposição — disponíveis em Configurações → Privacidade.</p>
              <p><strong class="text-white">Contato DPO:</strong> privacidade&#64;manutex.com.br</p>
              <p class="text-xs text-slate-500">Versão {{ lgpd.PRIVACY_POLICY_VERSION }} · Vigência: 26/05/2026</p>
            </div>
            <label class="flex items-start gap-3 cursor-pointer group">
              <input
                type="checkbox"
                [(ngModel)]="ppAccepted"
                class="mt-1 w-4 h-4 accent-petroleum-500 cursor-pointer">
              <span class="text-sm text-slate-300 group-hover:text-white transition-colors">
                Li e aceito a
                <a routerLink="/politica-privacidade" target="_blank" class="text-petroleum-400 hover:underline">Política de Privacidade</a>
                do PitStop Manager.
              </span>
            </label>
          </div>

          <div class="border-t border-surface-700"></div>

          <!-- Termos de Uso -->
          <div class="space-y-3">
            <h2 class="text-base font-semibold text-white">Termos de Uso</h2>
            <div class="bg-surface-800 rounded-xl p-4 text-sm text-slate-300 max-h-48 overflow-y-auto leading-relaxed space-y-2">
              <p>Ao usar o PitStop Manager você concorda em utilizar o serviço apenas para fins lícitos relacionados à gestão de oficinas mecânicas.</p>
              <p><strong class="text-white">Responsabilidades:</strong> Você é responsável pela veracidade dos dados inseridos e pelo acesso de seus colaboradores.</p>
              <p><strong class="text-white">Proibições:</strong> É vedado compartilhar credenciais, inserir dados falsos ou tentar acessar dados de outros tenants.</p>
              <p><strong class="text-white">SLA:</strong> O serviço é fornecido "como está" com disponibilidade alvo de 99,5% (exceto manutenção programada).</p>
              <p><strong class="text-white">Rescisão:</strong> Qualquer parte pode encerrar o contrato com 30 dias de aviso. Os dados serão exportados e removidos conforme a LGPD.</p>
              <p class="text-xs text-slate-500">Versão {{ lgpd.TERMS_OF_USE_VERSION }} · Vigência: 26/05/2026</p>
            </div>
            <label class="flex items-start gap-3 cursor-pointer group">
              <input
                type="checkbox"
                [(ngModel)]="touAccepted"
                class="mt-1 w-4 h-4 accent-petroleum-500 cursor-pointer">
              <span class="text-sm text-slate-300 group-hover:text-white transition-colors">
                Li e aceito os
                <a routerLink="/termos-de-uso" target="_blank" class="text-petroleum-400 hover:underline">Termos de Uso</a>
                do PitStop Manager.
              </span>
            </label>
          </div>

          <!-- Error -->
          @if (errorMessage()) {
            <div class="alert-danger text-sm">{{ errorMessage() }}</div>
          }

          <!-- Actions -->
          <div class="flex flex-col sm:flex-row gap-3 pt-2">
            <button
              (click)="accept()"
              [disabled]="!ppAccepted || !touAccepted || loading()"
              class="btn-primary flex-1 py-3">
              @if (loading()) {
                <svg class="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Salvando...
              } @else {
                Aceitar e continuar
              }
            </button>
            <button (click)="decline()" class="btn-secondary flex-1 py-3 text-sm">
              Recusar e sair
            </button>
          </div>

          <p class="text-center text-xs text-slate-600">
            Você pode revogar o consentimento a qualquer momento em
            <strong>Configurações → Privacidade</strong>.
          </p>
        </div>
      </div>
    </div>
  `,
})
export class ConsentComponent {
  readonly lgpd   = inject(LgpdService);
  private route   = inject(ActivatedRoute);
  private router  = inject(Router);

  ppAccepted  = false;
  touAccepted = false;
  readonly loading      = signal(false);
  readonly errorMessage = signal<string | null>(null);

  accept(): void {
    if (!this.ppAccepted || !this.touAccepted) return;
    this.loading.set(true);
    this.errorMessage.set(null);

    forkJoin([
      this.lgpd.recordConsent({
        policyType: 'PRIVACY_POLICY',
        policyVersion: this.lgpd.PRIVACY_POLICY_VERSION,
        accepted: true,
      }),
      this.lgpd.recordConsent({
        policyType: 'TERMS_OF_USE',
        policyVersion: this.lgpd.TERMS_OF_USE_VERSION,
        accepted: true,
      }),
    ]).subscribe({
      next: () => {
        this.loading.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
        this.router.navigateByUrl(returnUrl);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Não foi possível registrar o consentimento. Tente novamente.');
      },
    });
  }

  decline(): void {
    this.lgpd.invalidateConsentCache();
    this.router.navigate(['/login']);
  }
}
