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
    <div class="p-8">

      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">🚗 Veículos</h1>
          <p class="text-sm text-gray-500 mt-0.5">{{ totalElements() }} veículo(s) cadastrado(s)</p>
        </div>
        <a routerLink="/veiculos/novo" class="btn-primary">
          + Novo Veículo
        </a>
      </div>

      <!-- Busca -->
      <div class="mb-5">
        <input
          type="search"
          [(ngModel)]="searchTerm"
          (ngModelChange)="onSearch($event)"
          class="form-input max-w-sm"
          placeholder="Buscar por placa ou modelo...">
      </div>

      <!-- Tabela -->
      <div class="card p-0 overflow-hidden">
        @if (loading()) {
          <div class="py-16 text-center text-gray-400">Carregando...</div>
        } @else if (veiculos().length === 0) {
          <div class="py-16 text-center text-gray-400">
            Nenhum veículo encontrado.
          </div>
        } @else {
          <table class="w-full">
            <thead class="bg-gray-50 border-b border-gray-200">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Placa</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Veículo</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Chassi</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">RENAVAM</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Ano</th>
                <th class="px-6 py-3"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              @for (v of veiculos(); track v.id) {
                <tr class="hover:bg-gray-50 transition-colors">
                  <td class="px-6 py-4 font-mono font-semibold text-brand-700">{{ v.placa }}</td>
                  <td class="px-6 py-4 text-sm">
                    <p class="font-medium text-gray-900">{{ v.marca }} {{ v.modelo }}</p>
                    <p class="text-gray-500">{{ v.cor }}</p>
                  </td>
                  <!-- Dados sensíveis — exibidos mascarados por padrão -->
                  <td class="px-6 py-4 font-mono text-xs text-gray-500">{{ v.chassi }}</td>
                  <td class="px-6 py-4 font-mono text-xs text-gray-500">{{ v.renavam }}</td>
                  <td class="px-6 py-4 text-sm text-gray-600">
                    {{ v.anoFabricacao }}/{{ v.anoModelo }}
                  </td>
                  <td class="px-6 py-4 text-right">
                    <a [routerLink]="['/veiculos', v.id]"
                       class="text-brand-600 hover:text-brand-800 text-sm font-medium">
                      Ver →
                    </a>
                  </td>
                </tr>
              }
            </tbody>
          </table>

          <!-- Paginação simples -->
          @if (totalPages() > 1) {
            <div class="px-6 py-3 border-t border-gray-100 flex items-center justify-between text-sm">
              <span class="text-gray-500">Página {{ currentPage() + 1 }} de {{ totalPages() }}</span>
              <div class="flex gap-2">
                <button class="btn-secondary py-1 px-3" [disabled]="currentPage() === 0"
                  (click)="goToPage(currentPage() - 1)">← Anterior</button>
                <button class="btn-secondary py-1 px-3" [disabled]="currentPage() >= totalPages() - 1"
                  (click)="goToPage(currentPage() + 1)">Próxima →</button>
              </div>
            </div>
          }
        }
      </div>

      @if (!isPrivileged()) {
        <p class="mt-3 text-xs text-gray-400">
          🔒 Chassi e RENAVAM exibidos parcialmente. Contate um gerente para acesso completo.
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
