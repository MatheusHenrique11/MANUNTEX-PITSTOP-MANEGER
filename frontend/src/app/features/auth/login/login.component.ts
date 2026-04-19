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
    <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-brand-900 to-brand-700 px-4">
      <div class="w-full max-w-md">

        <!-- Header -->
        <div class="text-center mb-8">
          <span class="text-5xl">🔧</span>
          <h1 class="mt-3 text-3xl font-bold text-white">PitStop Manager</h1>
          <p class="mt-1 text-brand-200 text-sm">Sistema de controle de manutenções</p>
        </div>

        <!-- Card -->
        <div class="card">
          @if (sessionExpired()) {
            <div class="mb-4 px-3 py-2 bg-amber-50 border border-amber-200 rounded-lg text-sm text-amber-800">
              Sua sessão expirou. Por favor, faça login novamente.
            </div>
          }

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
                <p class="form-error">E-mail inválido</p>
              }
            </div>

            <div>
              <label class="form-label" for="password">Senha</label>
              <input
                id="password"
                type="password"
                formControlName="password"
                class="form-input"
                placeholder="••••••••"
                autocomplete="current-password">
              @if (form.get('password')?.invalid && form.get('password')?.touched) {
                <p class="form-error">Senha obrigatória (mínimo 8 caracteres)</p>
              }
            </div>

            @if (errorMessage()) {
              <div class="px-3 py-2 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                {{ errorMessage() }}
              </div>
            }

            <button
              type="submit"
              [disabled]="loading() || form.invalid"
              class="btn-primary w-full py-2.5">
              @if (loading()) {
                <span class="animate-spin mr-2">⟳</span> Entrando...
              } @else {
                Entrar
              }
            </button>
          </form>
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

  readonly form = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor() {
    this.route.queryParams.subscribe(params => {
      if (params['expired']) this.sessionExpired.set(true);
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
        this.errorMessage.set(err.detail ?? 'Erro ao fazer login. Tente novamente.');
      },
    });
  }
}
