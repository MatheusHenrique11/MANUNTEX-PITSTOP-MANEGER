import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NotificationService } from '@core/services/notification.service';
import {
  CHANNEL_LABELS,
  EVENT_LABELS,
  NotificationChannel,
  NotificationEvent,
  NotificationTemplate,
  TEMPLATE_VARIABLES,
} from '@core/models/notification.model';

@Component({
  selector: 'app-notification-templates',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-wrapper">
      <div class="page-header">
        <div>
          <h1 class="page-title">Templates de Notificação</h1>
          <p class="page-subtitle">Configure as mensagens enviadas por WhatsApp e e-mail</p>
        </div>
        <div class="flex gap-3">
          <a routerLink="/admin/notificacoes/config"
             class="btn-secondary text-sm">Configurar Provedor</a>
          <a routerLink="/admin/notificacoes/logs"
             class="btn-secondary text-sm">Ver Logs</a>
          <button (click)="seed()" [disabled]="seeding()"
                  class="btn-primary text-sm">
            {{ seeding() ? 'Criando...' : 'Criar Templates Padrão' }}
          </button>
        </div>
      </div>

      <!-- Variáveis disponíveis -->
      <div class="card mb-6 p-4 bg-surface-800/50">
        <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
          Variáveis disponíveis nos templates
        </p>
        <div class="flex flex-wrap gap-2">
          @for (v of vars; track v.key) {
            <span class="text-xs font-mono bg-surface-700 text-petroleum-300
                         px-2 py-1 rounded border border-surface-600"
                  title="{{ v.desc }}">{{ v.key }}</span>
          }
        </div>
      </div>

      @if (loading()) {
        <div class="card text-center py-12 text-slate-400">Carregando templates...</div>
      } @else if (templates().length === 0) {
        <div class="card text-center py-16">
          <p class="text-slate-400 mb-4">Nenhum template encontrado.</p>
          <button (click)="seed()" class="btn-primary text-sm">Criar Templates Padrão</button>
        </div>
      } @else {
        @for (evento of eventos; track evento) {
          <div class="card mb-4">
            <h3 class="text-sm font-semibold text-white mb-4 flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-petroleum-500"></span>
              {{ eventLabel(evento) }}
            </h3>
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
              @for (canal of canais; track canal) {
                @if (getTemplate(evento, canal); as tmpl) {
                  <div class="border border-surface-700 rounded-lg p-4">
                    <div class="flex items-center justify-between mb-3">
                      <span class="text-xs font-semibold text-slate-400 uppercase">
                        {{ channelLabel(canal) }}
                      </span>
                      <label class="flex items-center gap-2 cursor-pointer">
                        <span class="text-xs text-slate-500">{{ tmpl.ativo ? 'Ativo' : 'Inativo' }}</span>
                        <div class="relative">
                          <input type="checkbox" class="sr-only"
                                 [checked]="tmpl.ativo"
                                 (change)="toggleAtivo(tmpl)">
                          <div class="w-9 h-5 rounded-full transition-colors"
                               [class]="tmpl.ativo ? 'bg-petroleum-600' : 'bg-surface-600'">
                            <div class="absolute top-0.5 w-4 h-4 bg-white rounded-full
                                        shadow transition-transform"
                                 [class]="tmpl.ativo ? 'left-4' : 'left-0.5'"></div>
                          </div>
                        </div>
                      </label>
                    </div>

                    @if (editingId() === tmpl.id) {
                      <input [(ngModel)]="editTitulo" placeholder="Título (opcional)"
                             class="form-input text-xs mb-2 w-full" />
                      <textarea [(ngModel)]="editCorpo" rows="4"
                                class="form-input text-xs w-full resize-y mb-3"></textarea>
                      <div class="flex gap-2">
                        <button (click)="save(tmpl)" [disabled]="saving()"
                                class="btn-primary text-xs py-1.5">
                          {{ saving() ? 'Salvando...' : 'Salvar' }}
                        </button>
                        <button (click)="cancelEdit()"
                                class="btn-secondary text-xs py-1.5">Cancelar</button>
                      </div>
                    } @else {
                      @if (tmpl.titulo) {
                        <p class="text-xs font-medium text-slate-300 mb-1">{{ tmpl.titulo }}</p>
                      }
                      <p class="text-xs text-slate-500 whitespace-pre-wrap leading-relaxed">
                        {{ tmpl.corpo }}
                      </p>
                      <button (click)="startEdit(tmpl)"
                              class="mt-3 text-xs text-petroleum-400 hover:text-petroleum-300 transition-colors">
                        Editar →
                      </button>
                    }
                  </div>
                }
              }
            </div>
          </div>
        }
      }
    </div>
  `,
})
export class NotificationTemplatesComponent implements OnInit {
  private svc = inject(NotificationService);

  readonly loading    = signal(true);
  readonly saving     = signal(false);
  readonly seeding    = signal(false);
  readonly templates  = signal<NotificationTemplate[]>([]);
  readonly editingId  = signal<string | null>(null);

  editTitulo = '';
  editCorpo  = '';

  readonly vars    = TEMPLATE_VARIABLES;
  readonly eventos = Object.keys(EVENT_LABELS) as NotificationEvent[];
  readonly canais  = ['WHATSAPP', 'EMAIL'] as NotificationChannel[];

  ngOnInit() { this.load(); }

  load() {
    this.svc.getTemplates().subscribe({
      next:  t  => { this.templates.set(t); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  seed() {
    this.seeding.set(true);
    this.svc.seedTemplates().subscribe({
      next:  () => { this.seeding.set(false); this.load(); },
      error: () => this.seeding.set(false),
    });
  }

  getTemplate(evento: NotificationEvent, canal: NotificationChannel) {
    return this.templates().find(t => t.evento === evento && t.canal === canal) ?? null;
  }

  startEdit(t: NotificationTemplate) {
    this.editingId.set(t.id);
    this.editTitulo = t.titulo ?? '';
    this.editCorpo  = t.corpo;
  }

  cancelEdit() { this.editingId.set(null); }

  save(t: NotificationTemplate) {
    this.saving.set(true);
    this.svc.updateTemplate(t.id, { titulo: this.editTitulo || null, corpo: this.editCorpo, ativo: t.ativo })
      .subscribe({
        next: updated => {
          this.templates.update(list => list.map(x => x.id === updated.id ? updated : x));
          this.editingId.set(null);
          this.saving.set(false);
        },
        error: () => this.saving.set(false),
      });
  }

  toggleAtivo(t: NotificationTemplate) {
    this.svc.updateTemplate(t.id, { titulo: t.titulo, corpo: t.corpo, ativo: !t.ativo })
      .subscribe(updated =>
        this.templates.update(list => list.map(x => x.id === updated.id ? updated : x))
      );
  }

  eventLabel(e: NotificationEvent)   { return EVENT_LABELS[e]   ?? e; }
  channelLabel(c: NotificationChannel) { return CHANNEL_LABELS[c] ?? c; }
}
