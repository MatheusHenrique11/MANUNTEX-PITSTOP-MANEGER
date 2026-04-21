import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { RastreioService } from '@core/services/rastreio.service';
import { RastreioResponse, STATUS_CONFIG } from '@core/models/rastreio.model';
import { StatusManutencao } from '@core/models/manutencao.model';

const STEPS: StatusManutencao[] = ['ABERTA', 'EM_ANDAMENTO', 'AGUARDANDO_PECAS', 'CONCLUIDA'];

@Component({
  selector: 'app-rastreio',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="min-h-screen bg-surface-950 flex flex-col">

      <!-- Header mínimo -->
      <header class="px-5 py-4 border-b border-surface-700 bg-surface-900 flex items-center gap-3">
        <div class="w-8 h-8 bg-petroleum-700 rounded-lg flex items-center justify-center shadow-glow-petroleum">
          <svg viewBox="0 0 24 24" class="w-4 h-4 text-white fill-current">
            <path d="M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z"/>
          </svg>
        </div>
        <div>
          <p class="text-sm font-bold text-white leading-tight">PitStop Manager</p>
          <p class="text-xs text-slate-500 leading-tight">Rastreio de Serviço</p>
        </div>
      </header>

      <!-- Conteúdo -->
      <main class="flex-1 flex items-start justify-center px-4 py-10">
        <div class="w-full max-w-lg space-y-6">

          <!-- Carregando -->
          @if (loading()) {
            <div class="card flex flex-col items-center gap-4 py-12">
              <div class="w-8 h-8 border-2 border-petroleum-500 border-t-transparent rounded-full animate-spin"></div>
              <p class="text-slate-400 text-sm">Buscando sua ordem de serviço...</p>
            </div>
          }

          <!-- Erro -->
          @if (!loading() && erro()) {
            <div class="card flex flex-col items-center gap-4 py-12 text-center">
              <div class="w-14 h-14 rounded-full bg-danger-600/15 flex items-center justify-center">
                <svg viewBox="0 0 24 24" class="w-7 h-7 fill-current text-danger-400">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                </svg>
              </div>
              <div>
                <p class="font-semibold text-white">Link não encontrado</p>
                <p class="text-sm text-slate-400 mt-1">Este link de rastreio é inválido ou foi removido.</p>
              </div>
            </div>
          }

          <!-- Dados da OS -->
          @if (!loading() && os()) {
            <!-- Status badge principal -->
            <div class="card text-center space-y-2 py-7">
              <p class="text-xs text-slate-500 uppercase tracking-widest">Status atual</p>
              <p class="text-3xl font-bold" [class]="statusConfig().color">
                {{ statusConfig().label }}
              </p>
              @if (os()!.status === 'CONCLUIDA' && os()!.dataConclusao) {
                <p class="text-xs text-slate-500">
                  Concluído em {{ os()!.dataConclusao | date:'dd/MM/yyyy' }}
                </p>
              }
            </div>

            <!-- Timeline de progresso -->
            @if (os()!.status !== 'CANCELADA') {
              <div class="card">
                <p class="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-5">Progresso</p>
                <div class="flex items-center">
                  @for (step of STEPS; track step; let i = $index) {
                    <!-- Passo -->
                    <div class="flex flex-col items-center flex-1">
                      <div class="w-8 h-8 rounded-full border-2 flex items-center justify-center text-xs font-bold transition-all"
                           [class]="stepClass(step)">
                        @if (isPast(step)) { ✓ } @else { {{ i + 1 }} }
                      </div>
                      <p class="text-center mt-1.5 leading-tight"
                         [class]="stepLabelClass(step)"
                         style="font-size:0.65rem">
                        {{ STATUS_CONFIG[step].label }}
                      </p>
                    </div>
                    <!-- Linha conectora -->
                    @if (i < STEPS.length - 1) {
                      <div class="flex-1 h-0.5 -mt-5 mx-1"
                           [class]="isPast(STEPS[i + 1]) || isCurrent(STEPS[i + 1]) ? 'bg-petroleum-600' : 'bg-surface-700'">
                      </div>
                    }
                  }
                </div>
              </div>
            }

            <!-- Informações do veículo -->
            <div class="card space-y-3">
              <p class="text-xs font-semibold text-slate-500 uppercase tracking-widest">Veículo</p>
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 bg-surface-700 rounded-lg flex items-center justify-center flex-shrink-0">
                  <svg viewBox="0 0 24 24" class="w-5 h-5 fill-current text-slate-400">
                    <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/>
                  </svg>
                </div>
                <div>
                  <p class="font-semibold text-white text-sm">
                    {{ os()!.veiculoMarca }} {{ os()!.veiculoModelo }}
                    @if (os()!.veiculoCor) { <span class="text-slate-500">· {{ os()!.veiculoCor }}</span> }
                  </p>
                  <p class="font-mono text-xs bg-surface-700 text-petroleum-400 px-2 py-0.5 rounded mt-0.5 inline-block">
                    {{ os()!.veiculoPlaca }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Descrição do serviço -->
            <div class="card space-y-3">
              <p class="text-xs font-semibold text-slate-500 uppercase tracking-widest">Serviço</p>
              <p class="text-sm text-slate-200 leading-relaxed">{{ os()!.descricao }}</p>
              @if (os()!.observacoes) {
                <div class="border-t border-surface-700 pt-3">
                  <p class="text-xs text-slate-500 mb-1">Observações</p>
                  <p class="text-sm text-slate-400 leading-relaxed">{{ os()!.observacoes }}</p>
                </div>
              }
            </div>

            <!-- Rodapé: responsável + data -->
            <div class="flex gap-3">
              <div class="card flex-1 space-y-1">
                <p class="text-xs text-slate-500">Responsável</p>
                <p class="text-sm font-semibold text-white">{{ os()!.mecanicoNome }}</p>
              </div>
              <div class="card flex-1 space-y-1">
                <p class="text-xs text-slate-500">Entrada</p>
                <p class="text-sm font-semibold text-white">{{ os()!.dataEntrada | date:'dd/MM/yyyy' }}</p>
              </div>
            </div>

            <p class="text-center text-xs text-slate-600 pb-4">
              PitStop Manager · Manutex
            </p>
          }

        </div>
      </main>
    </div>
  `,
})
export class RastreioComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private rastreioService = inject(RastreioService);

  readonly STEPS = STEPS;
  readonly STATUS_CONFIG = STATUS_CONFIG;

  readonly loading = signal(true);
  readonly os = signal<RastreioResponse | null>(null);
  readonly erro = signal(false);

  ngOnInit() {
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) { this.erro.set(true); this.loading.set(false); return; }

    this.rastreioService.buscar(token).subscribe({
      next: data => { this.os.set(data); this.loading.set(false); },
      error: () => { this.erro.set(true); this.loading.set(false); },
    });
  }

  statusConfig() {
    return STATUS_CONFIG[this.os()!.status];
  }

  isPast(step: StatusManutencao): boolean {
    const current = STATUS_CONFIG[this.os()!.status].step;
    return STATUS_CONFIG[step].step < current;
  }

  isCurrent(step: StatusManutencao): boolean {
    return this.os()!.status === step;
  }

  stepClass(step: StatusManutencao): string {
    if (this.isCurrent(step)) return 'border-petroleum-500 bg-petroleum-700/30 text-petroleum-400';
    if (this.isPast(step))    return 'border-success-500 bg-success-600/20 text-success-400';
    return 'border-surface-600 bg-surface-800 text-slate-600';
  }

  stepLabelClass(step: StatusManutencao): string {
    if (this.isCurrent(step)) return 'text-petroleum-400';
    if (this.isPast(step))    return 'text-success-400';
    return 'text-slate-600';
  }
}
