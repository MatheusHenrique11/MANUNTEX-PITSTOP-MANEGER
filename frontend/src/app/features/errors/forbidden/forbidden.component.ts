import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-surface-950 flex items-center justify-center px-4">
      <div class="text-center max-w-md">
        <div class="w-20 h-20 bg-danger-600/20 border border-danger-600/30 rounded-full
                    flex items-center justify-center mx-auto mb-6">
          <svg viewBox="0 0 24 24" class="w-10 h-10 fill-current text-danger-400">
            <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 4l3.5 6h-7L12 5zm0 10a2 2 0 1 1 0-4 2 2 0 0 1 0 4z"/>
          </svg>
        </div>
        <h1 class="text-5xl font-extrabold text-white mb-2">403</h1>
        <p class="text-lg font-semibold text-slate-300 mb-2">Acesso Negado</p>
        <p class="text-sm text-slate-500 mb-8">
          Você não tem permissão para acessar esta página.
          Entre em contato com o administrador se acreditar que isso é um erro.
        </p>
        <a routerLink="/dashboard"
           class="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-petroleum-700
                  hover:bg-petroleum-600 text-white text-sm font-medium transition-colors duration-150">
          <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current">
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
          </svg>
          Voltar ao Dashboard
        </a>
      </div>
    </div>
  `,
})
export class ForbiddenComponent {}
