import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="min-h-screen flex bg-surface-950">

      <!-- Left panel: branding -->
      <div class="hidden lg:flex flex-col justify-between w-1/2 xl:w-2/5
                  bg-surface-900 border-r border-surface-700 p-12 relative overflow-hidden">

        <!-- Decorative grid -->
        <div class="absolute inset-0 opacity-5"
             style="background-image: linear-gradient(rgba(8,145,178,.5) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(8,145,178,.5) 1px, transparent 1px);
                    background-size: 40px 40px;"></div>

        <!-- Decorative blobs -->
        <div class="absolute top-1/4 -left-20 w-72 h-72 bg-petroleum-700/20 rounded-full blur-3xl"></div>
        <div class="absolute bottom-1/4 -right-10 w-56 h-56 bg-safety-600/15 rounded-full blur-3xl"></div>

        <!-- Logo -->
        <div class="relative z-10 flex items-center gap-3">
          <div class="w-10 h-10 bg-petroleum-700 rounded-xl flex items-center justify-center shadow-glow-petroleum">
            <svg viewBox="0 0 24 24" class="w-6 h-6 text-white fill-current">
              <path d="M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z"/>
            </svg>
          </div>
          <div>
            <p class="font-bold text-white text-lg leading-none">PitStop Manager</p>
            <p class="text-xs text-slate-500">by Manutex</p>
          </div>
        </div>

        <!-- Hero text -->
        <div class="relative z-10 space-y-6">
          <div>
            <h2 class="text-4xl xl:text-5xl font-extrabold text-white leading-tight tracking-tight">
              Gestão de<br>
              <span class="text-petroleum-400">Manutenção</span><br>
              Industrial
            </h2>
            <p class="mt-4 text-slate-400 text-base leading-relaxed max-w-sm">
              Controle ordens de serviço, frotas e documentos com segurança e eficiência em campo.
            </p>
          </div>

          <!-- Feature chips -->
          <div class="flex flex-wrap gap-2">
            @for (tag of tags; track tag) {
              <span class="badge-info text-xs">{{ tag }}</span>
            }
          </div>
        </div>

        <!-- Footer -->
        <p class="relative z-10 text-xs text-slate-600">
          © {{ year }} Manutex · Sistema SaaS de Oficina
        </p>
      </div>

      <!-- Right panel: login form -->
      <div class="flex-1 flex items-center justify-center px-6 py-12">
        <div class="w-full max-w-sm space-y-8">

          <!-- Mobile logo -->
          <div class="lg:hidden text-center">
            <div class="w-12 h-12 bg-petroleum-700 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-glow-petroleum">
              <svg viewBox="0 0 24 24" class="w-7 h-7 text-white fill-current">
                <path d="M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z"/>
              </svg>
            </div>
            <h1 class="text-2xl font-bold text-white">PitStop Manager</h1>
          </div>

          <!-- Heading -->
          <div>
            <h2 class="text-2xl font-bold text-white">Bem-vindo de volta</h2>
            <p class="text-sm text-slate-400 mt-1">Faça login para acessar o painel</p>
          </div>

          <!-- Session expired alert -->
          @if (sessionExpired()) {
            <div class="alert-warning">
              <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current flex-shrink-0 mt-0.5">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
              </svg>
              Sessão expirada. Faça login novamente.
            </div>
          }

          <!-- Form -->
          <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-5">

            <div>
              <label class="form-label" for="email">E-mail</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                class="form-input"
                placeholder="seu@email.com"
                autocomplete="username">
              @if (form.get('email')?.invalid && form.get('email')?.touched) {
                <p class="form-error">
                  <svg viewBox="0 0 24 24" class="w-3 h-3 fill-current"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                  E-mail inválido
                </p>
              }
            </div>

            <div>
              <label class="form-label" for="password">Senha</label>
              <div class="relative">
                <input
                  id="password"
                  [type]="showPassword() ? 'text' : 'password'"
                  formControlName="password"
                  class="form-input pr-11"
                  placeholder="••••••••"
                  autocomplete="current-password">
                <button
                  type="button"
                  (click)="showPassword.set(!showPassword())"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors">
                  @if (showPassword()) {
                    <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current">
                      <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>
                    </svg>
                  } @else {
                    <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current">
                      <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                    </svg>
                  }
                </button>
              </div>
              @if (form.get('password')?.invalid && form.get('password')?.touched) {
                <p class="form-error">
                  <svg viewBox="0 0 24 24" class="w-3 h-3 fill-current"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                  Mínimo 8 caracteres
                </p>
              }
            </div>

            @if (errorMessage()) {
              <div class="alert-danger">
                <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current flex-shrink-0 mt-0.5">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                </svg>
                {{ errorMessage() }}
              </div>
            }

            <button
              type="submit"
              [disabled]="loading() || form.invalid"
              class="btn-primary w-full py-3 text-base">
              @if (loading()) {
                <svg class="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Autenticando...
              } @else {
                Entrar no sistema
              }
            </button>
          </form>

          <!-- Security note -->
          <p class="text-center text-xs text-slate-600">
            🔒 Conexão protegida com JWT · AES-256
          </p>
        </div>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly sessionExpired = signal(false);
  readonly showPassword = signal(false);

  readonly year = new Date().getFullYear();
  readonly tags = ['OS Digitais', 'Frota', 'Documentos Criptografados', 'Multi-usuário'];

  readonly form = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor() {
    this.route.queryParams.subscribe(p => {
      if (p['expired']) this.sessionExpired.set(true);
    });
  }

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set(null);
    const { email, password } = this.form.value;
    this.auth.login({ email: email!, password: password! }).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.detail ?? 'Credenciais inválidas. Tente novamente.');
      },
    });
  }
}
