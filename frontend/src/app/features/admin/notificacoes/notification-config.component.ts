import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NotificationService } from '@core/services/notification.service';
import { NotificationConfig } from '@core/models/notification.model';

@Component({
  selector: 'app-notification-config',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-wrapper">
      <div class="page-header">
        <div>
          <h1 class="page-title">Configuração de Notificações</h1>
          <p class="page-subtitle">Configure os provedores de WhatsApp e e-mail</p>
        </div>
        <a routerLink="/admin/notificacoes/templates" class="btn-secondary text-sm">
          ← Voltar para Templates
        </a>
      </div>

      @if (loading()) {
        <div class="card text-center py-12 text-slate-400">Carregando...</div>
      } @else {
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

          <!-- WhatsApp -->
          <div class="card">
            <div class="flex items-center gap-3 mb-5">
              <div class="w-10 h-10 rounded-xl bg-green-600/20 border border-green-600/30
                          flex items-center justify-center flex-shrink-0">
                <span class="text-lg">💬</span>
              </div>
              <div>
                <h2 class="text-sm font-semibold text-white">WhatsApp</h2>
                <span class="text-xs" [class]="config()?.whatsappConfigured ? 'text-green-400' : 'text-slate-500'">
                  {{ config()?.whatsappConfigured ? 'Configurado' : 'Não configurado' }}
                </span>
              </div>
            </div>

            <div class="space-y-3">
              <div>
                <label class="form-label text-xs">URL da Evolution API</label>
                <input [(ngModel)]="form.whatsappProviderUrl"
                       placeholder="https://api.evolution.suaempresa.com"
                       class="form-input text-sm" />
              </div>
              <div>
                <label class="form-label text-xs">API Token</label>
                <input [(ngModel)]="form.whatsappApiToken" type="password"
                       placeholder="••••••••••"
                       class="form-input text-sm" />
                <p class="text-xs text-slate-600 mt-1">Deixe em branco para não alterar</p>
              </div>
              <div>
                <label class="form-label text-xs">Nome da Instância</label>
                <input [(ngModel)]="form.whatsappInstanceName"
                       placeholder="minha-oficina"
                       class="form-input text-sm" />
              </div>
            </div>
          </div>

          <!-- E-mail -->
          <div class="card">
            <div class="flex items-center gap-3 mb-5">
              <div class="w-10 h-10 rounded-xl bg-petroleum-600/20 border border-petroleum-600/30
                          flex items-center justify-center flex-shrink-0">
                <span class="text-lg">✉️</span>
              </div>
              <div>
                <h2 class="text-sm font-semibold text-white">E-mail</h2>
                <span class="text-xs" [class]="config()?.emailConfigured ? 'text-green-400' : 'text-slate-500'">
                  {{ config()?.emailConfigured ? 'Configurado' : 'Não configurado' }}
                </span>
              </div>
            </div>

            <div>
              <label class="form-label text-xs">Remetente</label>
              <input [(ngModel)]="form.notificationEmailFrom"
                     placeholder="Oficina XYZ <noreply@oficina.com.br>"
                     class="form-input text-sm" />
              <p class="text-xs text-slate-600 mt-1">
                Servidor SMTP configurado pelo administrador do sistema.
              </p>
            </div>
          </div>
        </div>

        <div class="mt-6 flex justify-end">
          <button (click)="save()" [disabled]="saving()"
                  class="btn-primary">
            {{ saving() ? 'Salvando...' : 'Salvar Configurações' }}
          </button>
        </div>

        @if (saved()) {
          <div class="mt-4 alert-success">Configurações salvas com sucesso!</div>
        }
      }
    </div>
  `,
})
export class NotificationConfigComponent implements OnInit {
  private svc = inject(NotificationService);

  readonly loading = signal(true);
  readonly saving  = signal(false);
  readonly saved   = signal(false);
  readonly config  = signal<NotificationConfig | null>(null);

  form = {
    whatsappProviderUrl:   '',
    whatsappApiToken:      '',
    whatsappInstanceName:  '',
    notificationEmailFrom: '',
  };

  ngOnInit() {
    this.svc.getConfig().subscribe({
      next: c => {
        this.config.set(c);
        this.form.whatsappProviderUrl   = c.whatsappProviderUrl   ?? '';
        this.form.notificationEmailFrom = c.notificationEmailFrom ?? '';
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  save() {
    this.saving.set(true);
    this.svc.saveConfig({
      whatsappProviderUrl:   this.form.whatsappProviderUrl   || undefined,
      whatsappApiToken:      this.form.whatsappApiToken      || undefined,
      whatsappInstanceName:  this.form.whatsappInstanceName  || undefined,
      notificationEmailFrom: this.form.notificationEmailFrom || undefined,
    }).subscribe({
      next: c => {
        this.config.set(c);
        this.saving.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 3000);
      },
      error: () => this.saving.set(false),
    });
  }
}
