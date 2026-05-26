import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LgpdService } from '@core/services/lgpd.service';
import {
  ConsentStatusResponse,
  DsarResponse,
  DsarType,
  DSAR_TYPE_LABELS,
  DSAR_STATUS_LABELS,
  MyDataExport,
} from '@core/models/lgpd.model';

@Component({
  selector: 'app-lgpd-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="max-w-3xl mx-auto space-y-8 py-8 px-4">

      <!-- Header -->
      <div>
        <h1 class="text-2xl font-bold text-white">Privacidade e Dados Pessoais</h1>
        <p class="text-sm text-slate-400 mt-1">
          Gerencie seus consentimentos e exerça seus direitos conforme a
          <span class="text-petroleum-400">LGPD (Lei 13.709/2018)</span>.
        </p>
      </div>

      <!-- ── Status de consentimento ────────────────────────────────────────── -->
      <section class="bg-surface-900 rounded-2xl border border-surface-700 p-6 space-y-4">
        <h2 class="text-base font-semibold text-white">Seus Consentimentos</h2>

        @if (consentStatus()) {
          <div class="space-y-3">
            <div class="flex items-center justify-between p-3 rounded-xl bg-surface-800">
              <div>
                <p class="text-sm font-medium text-white">Política de Privacidade</p>
                <p class="text-xs text-slate-500">v{{ consentStatus()!.currentPrivacyPolicyVersion }}</p>
              </div>
              @if (consentStatus()!.privacyPolicyAccepted) {
                <span class="badge-success text-xs">Aceita</span>
              } @else {
                <span class="badge-warning text-xs">Pendente</span>
              }
            </div>
            <div class="flex items-center justify-between p-3 rounded-xl bg-surface-800">
              <div>
                <p class="text-sm font-medium text-white">Termos de Uso</p>
                <p class="text-xs text-slate-500">v{{ consentStatus()!.currentTermsOfUseVersion }}</p>
              </div>
              @if (consentStatus()!.termsOfUseAccepted) {
                <span class="badge-success text-xs">Aceito</span>
              } @else {
                <span class="badge-warning text-xs">Pendente</span>
              }
            </div>
          </div>
        } @else {
          <div class="text-sm text-slate-500 animate-pulse">Carregando consentimentos...</div>
        }

        <div class="flex flex-wrap gap-2 text-xs pt-1">
          <a routerLink="/politica-privacidade" class="text-petroleum-400 hover:underline">Ver Política de Privacidade</a>
          <span class="text-slate-600">·</span>
          <a routerLink="/termos-de-uso" class="text-petroleum-400 hover:underline">Ver Termos de Uso</a>
        </div>
      </section>

      <!-- ── Portabilidade ──────────────────────────────────────────────────── -->
      <section class="bg-surface-900 rounded-2xl border border-surface-700 p-6 space-y-4">
        <h2 class="text-base font-semibold text-white">Portabilidade de Dados</h2>
        <p class="text-sm text-slate-400">
          Exporte uma cópia dos seus dados pessoais em formato JSON (Art. 18, V LGPD).
        </p>

        @if (exportData()) {
          <div class="bg-surface-800 rounded-xl p-4 space-y-2">
            <p class="text-xs text-slate-400">Exportado em {{ exportData()!.exportDate | date:'dd/MM/yyyy HH:mm' }}</p>
            <button (click)="downloadExport()" class="btn-secondary text-sm py-2 px-4">
              Baixar JSON
            </button>
          </div>
        } @else {
          <button
            (click)="exportData_fetch()"
            [disabled]="exportLoading()"
            class="btn-secondary text-sm py-2 px-4">
            @if (exportLoading()) {
              Exportando...
            } @else {
              Exportar meus dados
            }
          </button>
        }
      </section>

      <!-- ── Solicitar exercício de direito ─────────────────────────────────── -->
      <section class="bg-surface-900 rounded-2xl border border-surface-700 p-6 space-y-4">
        <h2 class="text-base font-semibold text-white">Solicitar Exercício de Direito</h2>
        <p class="text-sm text-slate-400">
          Envie uma solicitação. Respondemos em até 15 dias (Art. 18 LGPD).
        </p>

        <div class="space-y-3">
          <div>
            <label class="form-label">Tipo de solicitação</label>
            <select [(ngModel)]="dsarType" class="form-input">
              @for (entry of dsarTypeEntries; track entry[0]) {
                <option [value]="entry[0]">{{ entry[1] }}</option>
              }
            </select>
          </div>
          <div>
            <label class="form-label">Detalhes <span class="text-slate-500">(opcional)</span></label>
            <textarea
              [(ngModel)]="dsarNotes"
              rows="3"
              class="form-input resize-none"
              placeholder="Descreva sua solicitação..."></textarea>
          </div>

          @if (dsarError()) {
            <div class="alert-danger text-sm">{{ dsarError() }}</div>
          }
          @if (dsarSuccess()) {
            <div class="alert-success text-sm">Solicitação enviada. Você será notificado em até 15 dias.</div>
          }

          <button
            (click)="submitDsar()"
            [disabled]="dsarLoading()"
            class="btn-primary text-sm py-2 px-6">
            @if (dsarLoading()) { Enviando... } @else { Enviar solicitação }
          </button>
        </div>
      </section>

      <!-- ── Histórico de solicitações ──────────────────────────────────────── -->
      <section class="bg-surface-900 rounded-2xl border border-surface-700 p-6 space-y-4">
        <h2 class="text-base font-semibold text-white">Histórico de Solicitações</h2>

        @if (dsars().length === 0) {
          <p class="text-sm text-slate-500">Nenhuma solicitação enviada.</p>
        } @else {
          <div class="space-y-3">
            @for (d of dsars(); track d.id) {
              <div class="p-3 rounded-xl bg-surface-800 flex items-start justify-between gap-4">
                <div class="space-y-0.5">
                  <p class="text-sm font-medium text-white">{{ typeLabel(d.requestType) }}</p>
                  <p class="text-xs text-slate-500">Enviada: {{ d.requestedAt | date:'dd/MM/yyyy' }} · Prazo: {{ d.deadlineAt | date:'dd/MM/yyyy' }}</p>
                  @if (d.responseNotes) {
                    <p class="text-xs text-slate-400 mt-1">Resposta: {{ d.responseNotes }}</p>
                  }
                </div>
                <span [class]="statusClass(d.status)" class="text-xs shrink-0">
                  {{ statusLabel(d.status) }}
                </span>
              </div>
            }
          </div>
        }
      </section>

      <!-- ── Links legais ───────────────────────────────────────────────────── -->
      <div class="flex flex-wrap gap-4 text-xs text-slate-500 pb-4">
        <a routerLink="/politica-privacidade" class="hover:text-petroleum-400 transition-colors">Política de Privacidade</a>
        <a routerLink="/termos-de-uso" class="hover:text-petroleum-400 transition-colors">Termos de Uso</a>
        <span>DPO: privacidade&#64;manutex.com.br</span>
        <span>ANPD: <a href="https://www.gov.br/anpd" target="_blank" rel="noopener" class="hover:text-petroleum-400">gov.br/anpd</a></span>
      </div>
    </div>
  `,
})
export class LgpdPanelComponent implements OnInit {
  private lgpd = inject(LgpdService);

  readonly consentStatus  = signal<ConsentStatusResponse | null>(null);
  readonly dsars          = signal<DsarResponse[]>([]);
  readonly exportData     = signal<MyDataExport | null>(null);
  readonly exportLoading  = signal(false);
  readonly dsarLoading    = signal(false);
  readonly dsarError      = signal<string | null>(null);
  readonly dsarSuccess    = signal(false);

  dsarType: DsarType = 'ACCESS';
  dsarNotes = '';

  readonly dsarTypeEntries = Object.entries(DSAR_TYPE_LABELS) as [DsarType, string][];

  ngOnInit(): void {
    this.lgpd.getConsentStatus().subscribe(s => this.consentStatus.set(s));
    this.lgpd.listMyDsars().subscribe(d => this.dsars.set(d));
  }

  exportData_fetch(): void {
    this.exportLoading.set(true);
    this.lgpd.exportMyData().subscribe({
      next: data => { this.exportLoading.set(false); this.exportData.set(data); },
      error: ()   => this.exportLoading.set(false),
    });
  }

  downloadExport(): void {
    const data = this.exportData();
    if (!data) return;
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `meus-dados-pitstop-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  submitDsar(): void {
    this.dsarError.set(null);
    this.dsarSuccess.set(false);
    this.dsarLoading.set(true);
    this.lgpd.submitDsar({ requestType: this.dsarType, notes: this.dsarNotes || undefined }).subscribe({
      next: dsar => {
        this.dsarLoading.set(false);
        this.dsarSuccess.set(true);
        this.dsars.update(list => [dsar, ...list]);
        this.dsarNotes = '';
      },
      error: err => {
        this.dsarLoading.set(false);
        this.dsarError.set(err.error?.detail ?? 'Erro ao enviar solicitação.');
      },
    });
  }

  typeLabel(type: DsarType)   { return DSAR_TYPE_LABELS[type];   }
  statusLabel(s: string)      { return DSAR_STATUS_LABELS[s as keyof typeof DSAR_STATUS_LABELS] ?? s; }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      PENDING:     'badge-warning',
      IN_PROGRESS: 'badge-info',
      COMPLETED:   'badge-success',
      REJECTED:    'badge-danger',
    };
    return map[s] ?? 'badge-info';
  }
}
