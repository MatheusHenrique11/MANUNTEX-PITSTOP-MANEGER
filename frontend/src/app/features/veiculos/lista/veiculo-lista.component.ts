import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { VeiculoService } from '@core/services/veiculo.service';
import { AuthService } from '@core/services/auth.service';
import { Veiculo } from '@core/models/veiculo.model';
import { debounceTime, distinctUntilChanged, Subject, switchMap } from 'rxjs';

@Component({
  selector: 'app-veiculo-lista',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="page-wrapper">

      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Veículos</h1>
          <p class="page-subtitle">
            @if (loading()) { Carregando... }
            @else { {{ totalElements() }} veículo(s) cadastrado(s) }
          </p>
        </div>
        <a routerLink="/veiculos/novo" class="btn-accent">
          <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
          Novo Veículo
        </a>
      </div>

      <!-- Search + filters bar -->
      <div class="flex flex-col sm:flex-row gap-3 mb-6">
        <div class="relative flex-1 max-w-sm">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500 fill-current pointer-events-none"
               viewBox="0 0 24 24">
            <path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
          </svg>
          <input
            type="search"
            [(ngModel)]="searchTerm"
            (ngModelChange)="onSearch($event)"
            class="form-input pl-9"
            placeholder="Buscar placa, modelo...">
        </div>
      </div>

      <!-- Table -->
      <div class="table-container">
        @if (loading()) {
          <div class="loading-pulse">
            <svg class="animate-spin w-6 h-6 mx-auto mb-3 text-petroleum-500" viewBox="0 0 24 24" fill="none">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Carregando veículos...
          </div>
        } @else if (veiculos().length === 0) {
          <div class="empty-state">
            <svg viewBox="0 0 24 24" class="w-12 h-12 text-surface-600 fill-current">
              <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/>
            </svg>
            <p class="font-medium text-slate-400">Nenhum veículo encontrado</p>
            @if (searchTerm) {
              <p class="text-sm text-slate-600">Tente outro termo de busca</p>
            } @else {
              <a routerLink="/veiculos/novo" class="btn-accent mt-2">Cadastrar primeiro veículo</a>
            }
          </div>
        } @else {
          <table class="w-full">
            <thead class="table-header">
              <tr>
                <th class="th">Placa</th>
                <th class="th">Veículo</th>
                <th class="th hidden md:table-cell">Chassi</th>
                <th class="th hidden lg:table-cell">RENAVAM</th>
                <th class="th hidden sm:table-cell">Ano</th>
                <th class="th text-right">Ação</th>
              </tr>
            </thead>
            <tbody>
              @for (v of veiculos(); track v.id) {
                <tr class="tr-hover group">
                  <td class="td">
                    <span class="font-mono font-bold text-petroleum-400 bg-petroleum-900/30
                                 px-2 py-0.5 rounded text-xs tracking-wider">
                      {{ v.placa }}
                    </span>
                  </td>
                  <td class="td">
                    <p class="font-semibold text-slate-100">{{ v.marca }} {{ v.modelo }}</p>
                    <p class="text-xs text-slate-500 mt-0.5">{{ v.cor }}</p>
                  </td>
                  <td class="td hidden md:table-cell font-mono text-xs text-slate-500">{{ v.chassi }}</td>
                  <td class="td hidden lg:table-cell font-mono text-xs text-slate-500">{{ v.renavam }}</td>
                  <td class="td hidden sm:table-cell text-slate-400 text-xs">
                    {{ v.anoFabricacao }}/{{ v.anoModelo }}
                  </td>
                  <td class="td text-right">
                    <a [routerLink]="['/veiculos', v.id]"
                       class="inline-flex items-center gap-1 text-xs font-semibold
                              text-petroleum-500 hover:text-petroleum-300
                              group-hover:underline transition-colors">
                      Detalhes
                      <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current">
                        <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
                      </svg>
                    </a>
                  </td>
                </tr>
              }
            </tbody>
          </table>

          <!-- Pagination -->
          @if (totalPages() > 1) {
            <div class="px-5 py-4 border-t border-surface-700 flex items-center justify-between text-sm">
              <span class="text-slate-500 text-xs">
                Página {{ currentPage() + 1 }} de {{ totalPages() }}
                · {{ totalElements() }} registros
              </span>
              <div class="flex gap-2">
                <button
                  class="btn-secondary py-1.5 px-3 text-xs"
                  [disabled]="currentPage() === 0"
                  (click)="goToPage(currentPage() - 1)">
                  ← Anterior
                </button>
                <button
                  class="btn-secondary py-1.5 px-3 text-xs"
                  [disabled]="currentPage() >= totalPages() - 1"
                  (click)="goToPage(currentPage() + 1)">
                  Próxima →
                </button>
              </div>
            </div>
          }
        }
      </div>

      @if (!isPrivileged()) {
        <p class="mt-3 text-xs text-slate-600 flex items-center gap-1.5">
          <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current text-safety-600">
            <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
          </svg>
          Chassi e RENAVAM exibidos parcialmente. Contate um gerente para acesso completo.
        </p>
      }
    </div>
  `,
})
export class VeiculoListaComponent implements OnInit {
  private veiculoService = inject(VeiculoService);
  private auth = inject(AuthService);

  readonly loading = signal(true);
  readonly veiculos = signal<Veiculo[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly isPrivileged = this.auth.isPrivileged;

  searchTerm = '';
  private searchSubject = new Subject<string>();

  ngOnInit() {
    this.loadPage(0);
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(q => {
        this.loading.set(true);
        return this.veiculoService.listar(0, 20, q || undefined);
      })
    ).subscribe(page => {
      this.veiculos.set(page.content);
      this.totalElements.set(page.totalElements);
      this.totalPages.set(page.totalPages);
      this.currentPage.set(page.number);
      this.loading.set(false);
    });
  }

  onSearch(term: string) { this.searchSubject.next(term); }
  goToPage(page: number) { this.loadPage(page); }

  private loadPage(page: number) {
    this.loading.set(true);
    this.veiculoService.listar(page, 20, this.searchTerm || undefined).subscribe(p => {
      this.veiculos.set(p.content);
      this.totalElements.set(p.totalElements);
      this.totalPages.set(p.totalPages);
      this.currentPage.set(p.number);
      this.loading.set(false);
    });
  }
}
