import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NotificationService } from '@core/services/notification.service';
import {
  CHANNEL_LABELS,
  EVENT_LABELS,
  NotificationLog,
  STATUS_COLORS,
} from '@core/models/notification.model';

@Component({
  selector: 'app-notification-logs',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-wrapper">
      <div class="page-header">
        <div>
          <h1 class="page-title">Logs de Notificação</h1>
          <p class="page-subtitle">Histórico de envios de mensagens</p>
        </div>
        <a routerLink="/admin/notificacoes/templates" class="btn-secondary text-sm">
          ← Voltar para Templates
        </a>
      </div>

      @if (loading()) {
        <div class="card text-center py-12 text-slate-400">Carregando logs...</div>
      } @else if (logs().length === 0) {
        <div class="card text-center py-16">
          <span class="text-3xl">📭</span>
          <p class="text-slate-400 mt-3">Nenhum envio registrado ainda.</p>
        </div>
      } @else {
        <div class="card p-0 overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-surface-800 border-b border-surface-700">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">Data</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">Evento</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">Canal</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">Destinatário</th>
                  <th class="px-4 py-3 text-center text-xs font-semibold text-slate-500 uppercase w-28">Status</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">Detalhe</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-surface-700">
                @for (log of logs(); track log.id) {
                  <tr class="hover:bg-surface-800/50">
                    <td class="px-4 py-3 text-slate-400 text-xs whitespace-nowrap">
                      {{ log.createdAt | date:'dd/MM/yyyy HH:mm' : 'UTC' }}
                    </td>
                    <td class="px-4 py-3 text-slate-300 text-xs">{{ eventLabel(log) }}</td>
                    <td class="px-4 py-3">
                      <span class="text-xs font-medium" [class]="channelColor(log)">
                        {{ channelLabel(log) }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-slate-400 font-mono text-xs">
                      {{ log.destinatario ?? '—' }}
                    </td>
                    <td class="px-4 py-3 text-center">
                      <span class="badge text-xs" [class]="statusColor(log)">
                        {{ log.status }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-slate-500 text-xs max-w-xs truncate">
                      {{ log.errorMessage ?? '—' }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>

        <!-- Paginação -->
        <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
          <span>{{ totalElements() }} registro(s)</span>
          <div class="flex gap-2">
            <button (click)="prevPage()" [disabled]="page() === 0"
                    class="btn-secondary text-xs py-1.5 disabled:opacity-40">← Anterior</button>
            <span class="px-3 py-1.5 text-xs">Pág. {{ page() + 1 }} / {{ totalPages() }}</span>
            <button (click)="nextPage()" [disabled]="page() + 1 >= totalPages()"
                    class="btn-secondary text-xs py-1.5 disabled:opacity-40">Próxima →</button>
          </div>
        </div>
      }
    </div>
  `,
})
export class NotificationLogsComponent implements OnInit {
  private svc = inject(NotificationService);

  readonly loading       = signal(true);
  readonly logs          = signal<NotificationLog[]>([]);
  readonly page          = signal(0);
  readonly totalPages    = signal(0);
  readonly totalElements = signal(0);

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.svc.getLogs(this.page(), 20).subscribe({
      next: r => {
        this.logs.set(r.content);
        this.totalPages.set(r.totalPages);
        this.totalElements.set(r.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  prevPage() { if (this.page() > 0) { this.page.update(p => p - 1); this.load(); } }
  nextPage() { if (this.page() + 1 < this.totalPages()) { this.page.update(p => p + 1); this.load(); } }

  eventLabel(l: NotificationLog)   { return EVENT_LABELS[l.evento]   ?? l.evento; }
  channelLabel(l: NotificationLog) { return CHANNEL_LABELS[l.canal]  ?? l.canal; }
  statusColor(l: NotificationLog)  { return STATUS_COLORS[l.status]  ?? 'badge-inactive'; }
  channelColor(l: NotificationLog) {
    return l.canal === 'WHATSAPP' ? 'text-green-400' : 'text-petroleum-400';
  }
}
