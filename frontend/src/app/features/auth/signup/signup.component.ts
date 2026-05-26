import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged, switchMap, catchError, EMPTY } from 'rxjs';
import { TenantService } from '@core/services/tenant.service';
import { cnpjValidator, formatCnpj } from '@core/validators/cnpj.validator';

function confirmPasswordValidator(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const senha = group.get('gerenteSenha')?.value;
    const confirma = group.get('confirmarSenha')?.value;
    return senha && confirma && senha !== confirma ? { passwordMismatch: true } : null;
  };
}

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  template: `
    <div class="min-h-screen bg-surface-950 flex items-center justify-center px-4 py-12">
      <div class="w-full max-w-2xl space-y-8">

        <!-- Logo -->
        <div class="text-center">
          <div class="w-12 h-12 bg-petroleum-700 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-glow-petroleum">
            <svg viewBox="0 0 24 24" class="w-7 h-7 text-white fill-current">
              <path d="M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z"/>
            </svg>
          </div>
          <h1 class="text-2xl font-bold text-white">Cadastro de Empresa</h1>
          <p class="text-sm text-slate-400 mt-1">Crie sua conta no PitStop Manager</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-8 bg-surface-900 rounded-2xl border border-surface-700 p-8">

          <!-- ── Dados fiscais da empresa ───────────────────────────────── -->
          <section class="space-y-5">
            <h2 class="text-sm font-semibold text-petroleum-400 uppercase tracking-widest border-b border-surface-700 pb-2">
              Dados da Empresa
            </h2>

            <!-- CNPJ -->
            <div>
              <label class="form-label" for="cnpj">CNPJ</label>
              <div class="relative">
                <input
                  id="cnpj"
                  type="text"
                  formControlName="cnpj"
                  class="form-input pr-10"
                  placeholder="00.000.000/0000-00"
                  maxlength="18"
                  (input)="onCnpjInput($event)"
                  autocomplete="off">
                @if (lookingUpCnpj()) {
                  <svg class="animate-spin absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-petroleum-400" viewBox="0 0 24 24" fill="none">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                }
                @if (cnpjFound()) {
                  <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-green-400 fill-current" viewBox="0 0 24 24">
                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                  </svg>
                }
              </div>
              @if (form.get('cnpj')?.invalid && form.get('cnpj')?.touched) {
                <p class="form-error">CNPJ inválido — verifique os dígitos verificadores</p>
              }
              @if (lookupError()) {
                <p class="form-error">{{ lookupError() }}</p>
              }
            </div>

            <!-- Razão Social / Nome Fantasia -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="form-label" for="razaoSocial">Razão Social</label>
                <input id="razaoSocial" type="text" formControlName="razaoSocial" class="form-input" placeholder="Empresa Ltda.">
                @if (form.get('razaoSocial')?.invalid && form.get('razaoSocial')?.touched) {
                  <p class="form-error">Razão social é obrigatória</p>
                }
              </div>
              <div>
                <label class="form-label" for="nomeFantasia">Nome Fantasia <span class="text-slate-500">(opcional)</span></label>
                <input id="nomeFantasia" type="text" formControlName="nomeFantasia" class="form-input" placeholder="Nome comercial">
              </div>
            </div>

            <!-- E-mail e Telefone fiscais -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="form-label" for="emailFiscal">E-mail Fiscal <span class="text-slate-500">(opcional)</span></label>
                <input id="emailFiscal" type="email" formControlName="emailFiscal" class="form-input" placeholder="fiscal@empresa.com">
                @if (form.get('emailFiscal')?.invalid && form.get('emailFiscal')?.touched) {
                  <p class="form-error">E-mail inválido</p>
                }
              </div>
              <div>
                <label class="form-label" for="telefoneFiscal">Telefone <span class="text-slate-500">(opcional)</span></label>
                <input id="telefoneFiscal" type="tel" formControlName="telefoneFiscal" class="form-input" placeholder="(11) 99999-9999">
              </div>
            </div>
          </section>

          <!-- ── Endereço ────────────────────────────────────────────────── -->
          <section class="space-y-5">
            <h2 class="text-sm font-semibold text-petroleum-400 uppercase tracking-widest border-b border-surface-700 pb-2">
              Endereço Fiscal
            </h2>

            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div class="col-span-1">
                <label class="form-label" for="cep">CEP</label>
                <input id="cep" type="text" formControlName="cep" class="form-input" placeholder="00000-000" maxlength="9">
                @if (form.get('cep')?.invalid && form.get('cep')?.touched) {
                  <p class="form-error">CEP inválido</p>
                }
              </div>
              <div class="col-span-2 sm:col-span-3">
                <label class="form-label" for="logradouro">Logradouro</label>
                <input id="logradouro" type="text" formControlName="logradouro" class="form-input" placeholder="Rua, Av., Rod...">
                @if (form.get('logradouro')?.invalid && form.get('logradouro')?.touched) {
                  <p class="form-error">Logradouro é obrigatório</p>
                }
              </div>
            </div>

            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div>
                <label class="form-label" for="numero">Número</label>
                <input id="numero" type="text" formControlName="numero" class="form-input" placeholder="123">
                @if (form.get('numero')?.invalid && form.get('numero')?.touched) {
                  <p class="form-error">Obrigatório</p>
                }
              </div>
              <div class="col-span-1 sm:col-span-2">
                <label class="form-label" for="complemento">Complemento <span class="text-slate-500">(opcional)</span></label>
                <input id="complemento" type="text" formControlName="complemento" class="form-input" placeholder="Sala, andar...">
              </div>
              <div>
                <label class="form-label" for="bairro">Bairro</label>
                <input id="bairro" type="text" formControlName="bairro" class="form-input" placeholder="Bairro">
                @if (form.get('bairro')?.invalid && form.get('bairro')?.touched) {
                  <p class="form-error">Obrigatório</p>
                }
              </div>
            </div>

            <div class="grid grid-cols-3 gap-4">
              <div class="col-span-2">
                <label class="form-label" for="cidade">Cidade</label>
                <input id="cidade" type="text" formControlName="cidade" class="form-input" placeholder="São Paulo">
                @if (form.get('cidade')?.invalid && form.get('cidade')?.touched) {
                  <p class="form-error">Cidade é obrigatória</p>
                }
              </div>
              <div>
                <label class="form-label" for="uf">UF</label>
                <input id="uf" type="text" formControlName="uf" class="form-input uppercase" placeholder="SP" maxlength="2">
                @if (form.get('uf')?.invalid && form.get('uf')?.touched) {
                  <p class="form-error">UF inválida</p>
                }
              </div>
            </div>
          </section>

          <!-- ── Dados de acesso ─────────────────────────────────────────── -->
          <section class="space-y-5" formGroupName="credenciais">
            <h2 class="text-sm font-semibold text-petroleum-400 uppercase tracking-widest border-b border-surface-700 pb-2">
              Dados de Acesso do Gerente
            </h2>

            <div>
              <label class="form-label" for="gerenteNome">Nome completo</label>
              <input id="gerenteNome" type="text" formControlName="gerenteNome" class="form-input" placeholder="João da Silva">
              @if (form.get('credenciais.gerenteNome')?.invalid && form.get('credenciais.gerenteNome')?.touched) {
                <p class="form-error">Nome é obrigatório</p>
              }
            </div>

            <div>
              <label class="form-label" for="gerenteEmail">E-mail de acesso</label>
              <input id="gerenteEmail" type="email" formControlName="gerenteEmail" class="form-input" placeholder="gerente@empresa.com" autocomplete="username">
              @if (form.get('credenciais.gerenteEmail')?.invalid && form.get('credenciais.gerenteEmail')?.touched) {
                <p class="form-error">E-mail inválido</p>
              }
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="form-label" for="gerenteSenha">Senha</label>
                <input id="gerenteSenha" type="password" formControlName="gerenteSenha" class="form-input" placeholder="Mín. 8 caracteres" autocomplete="new-password">
                @if (form.get('credenciais.gerenteSenha')?.invalid && form.get('credenciais.gerenteSenha')?.touched) {
                  <p class="form-error">Mínimo 8 caracteres</p>
                }
              </div>
              <div>
                <label class="form-label" for="confirmarSenha">Confirmar senha</label>
                <input id="confirmarSenha" type="password" formControlName="confirmarSenha" class="form-input" placeholder="Repita a senha" autocomplete="new-password">
                @if (form.get('credenciais')?.hasError('passwordMismatch') && form.get('credenciais.confirmarSenha')?.touched) {
                  <p class="form-error">As senhas não coincidem</p>
                }
              </div>
            </div>
          </section>

          <!-- ── Erros / submit ─────────────────────────────────────────── -->
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
              Cadastrando...
            } @else {
              Criar conta
            }
          </button>

          <p class="text-center text-sm text-slate-400">
            Já tem uma conta?
            <a routerLink="/login" class="text-petroleum-400 hover:text-petroleum-300 font-medium">Fazer login</a>
          </p>
        </form>
      </div>
    </div>
  `,
})
export class SignupComponent {
  private fb = inject(FormBuilder);
  private tenant = inject(TenantService);
  private router = inject(Router);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly lookingUpCnpj = signal(false);
  readonly cnpjFound = signal(false);
  readonly lookupError = signal<string | null>(null);

  readonly form = this.fb.group({
    cnpj:           ['', [Validators.required, cnpjValidator()]],
    razaoSocial:    ['', [Validators.required, Validators.maxLength(200)]],
    nomeFantasia:   ['', Validators.maxLength(200)],
    cep:            ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
    logradouro:     ['', [Validators.required, Validators.maxLength(200)]],
    numero:         ['', [Validators.required, Validators.maxLength(20)]],
    complemento:    ['', Validators.maxLength(100)],
    bairro:         ['', [Validators.required, Validators.maxLength(100)]],
    cidade:         ['', [Validators.required, Validators.maxLength(100)]],
    uf:             ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
    codigoMunicipioIbge: [''],
    emailFiscal:    ['', [Validators.email, Validators.maxLength(180)]],
    telefoneFiscal: ['', Validators.maxLength(20)],
    credenciais: this.fb.group({
      gerenteNome:    ['', [Validators.required, Validators.maxLength(150)]],
      gerenteEmail:   ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
      gerenteSenha:   ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmarSenha: ['', Validators.required],
    }, { validators: confirmPasswordValidator() }),
  });

  onCnpjInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatCnpj(input.value);
    input.value = formatted;
    this.form.get('cnpj')!.setValue(formatted, { emitEvent: false });
    this.form.get('cnpj')!.markAsTouched();

    this.cnpjFound.set(false);
    this.lookupError.set(null);

    const digits = formatted.replace(/\D/g, '');
    if (digits.length === 14 && !this.form.get('cnpj')!.hasError('cnpj')) {
      this.triggerLookup(digits);
    }
  }

  private triggerLookup(cnpj: string): void {
    this.lookingUpCnpj.set(true);
    this.tenant.lookupCnpj(cnpj).subscribe({
      next: (data) => {
        this.lookingUpCnpj.set(false);
        this.cnpjFound.set(true);
        this.form.patchValue({
          razaoSocial:         data.razaoSocial ?? '',
          nomeFantasia:        data.nomeFantasia ?? '',
          cep:                 data.cep ? data.cep.replace(/(\d{5})(\d{3})/, '$1-$2') : '',
          logradouro:          data.logradouro ?? '',
          numero:              data.numero ?? '',
          complemento:         data.complemento ?? '',
          bairro:              data.bairro ?? '',
          cidade:              data.cidade ?? '',
          uf:                  data.uf ?? '',
          codigoMunicipioIbge: data.codigoMunicipioIbge ?? '',
          emailFiscal:         data.emailFiscal ?? '',
          telefoneFiscal:      data.telefoneFiscal ?? '',
        });
      },
      error: (err) => {
        this.lookingUpCnpj.set(false);
        if (err.status === 404) {
          this.lookupError.set('CNPJ não encontrado na Receita Federal. Preencha os dados manualmente.');
        } else {
          this.lookupError.set('Não foi possível consultar o CNPJ. Preencha os dados manualmente.');
        }
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const cred = this.form.get('credenciais')!.value;
    const v = this.form.value;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.tenant.register({
      cnpj:                v.cnpj!,
      razaoSocial:         v.razaoSocial!,
      nomeFantasia:        v.nomeFantasia || undefined,
      cep:                 v.cep!,
      logradouro:          v.logradouro!,
      numero:              v.numero!,
      complemento:         v.complemento || undefined,
      bairro:              v.bairro!,
      cidade:              v.cidade!,
      uf:                  v.uf!,
      codigoMunicipioIbge: v.codigoMunicipioIbge || undefined,
      emailFiscal:         v.emailFiscal || undefined,
      telefoneFiscal:      v.telefoneFiscal || undefined,
      gerenteNome:         cred.gerenteNome!,
      gerenteEmail:        cred.gerenteEmail!,
      gerenteSenha:        cred.gerenteSenha!,
    }).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registered: '1' } }),
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err.error?.detail ?? err.error?.title ?? 'Erro ao criar conta. Tente novamente.'
        );
      },
    });
  }
}
