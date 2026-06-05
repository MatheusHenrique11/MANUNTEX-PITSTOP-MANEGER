import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import {
  FiscalEnvironment,
  FiscalService,
  TenantFiscalConfigResponse,
  TenantFiscalConfigRequest,
} from '@core/services/fiscal.service';

@Component({
  selector: 'app-tenant-fiscal-config',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-wrapper">
      <div class="page-header">
        <div>
          <h1 class="page-title">Configuração Fiscal da Oficina</h1>
          <p class="page-subtitle">Dados fiscais da sua empresa para emissão de NFS-e aos clientes</p>
        </div>
      </div>

      <!-- Status cards -->
      @if (!editando()) {
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div class="card text-center">
            <p class="text-xs text-slate-500 mb-1">Emissão NFS-e</p>
            <span class="badge"
              [ngClass]="config()?.fiscalEnabled ? 'badge-active' : 'badge-inactive'">
              {{ config()?.fiscalEnabled ? 'Habilitada' : 'Desabilitada' }}
            </span>
          </div>
          <div class="card text-center">
            <p class="text-xs text-slate-500 mb-1">Configuração</p>
            <span class="badge"
              [ngClass]="config()?.readyForEmission ? 'badge-active' : 'badge-warning'">
              {{ config()?.readyForEmission ? 'Completa' : 'Incompleta' }}
            </span>
          </div>
          <div class="card text-center">
            <p class="text-xs text-slate-500 mb-1">Ambiente</p>
            <span class="badge"
              [ngClass]="config()?.ambienteFiscal === 'PRODUCAO' ? 'badge-active' : 'badge-warning'">
              {{ config()?.ambienteFiscal ?? 'Não configurado' }}
            </span>
          </div>
        </div>

        <!-- Aviso dados incompletos -->
        @if (config() && !config()!.readyForEmission) {
          <div class="mb-6 rounded-lg border border-amber-600/30 bg-amber-600/10 p-4">
            <p class="text-sm font-semibold text-amber-400 mb-2">Complete os dados para habilitar a emissão</p>
            <ul class="text-xs text-slate-400 list-disc list-inside space-y-1">
              @if (!config()!.cnpjMascarado) { <li>CNPJ</li> }
              @if (!config()!.inscricaoMunicipal) { <li>Inscrição Municipal</li> }
              @if (!config()!.codigoMunicipio) { <li>Código do Município (IBGE)</li> }
              @if (!config()!.codigoServicoMunicipal) { <li>Código de Serviço Municipal</li> }
              @if (!config()!.itemListaServico) { <li>Item da Lista de Serviço</li> }
            </ul>
          </div>
        }

        <!-- Dados cadastrados -->
        @if (config()) {
          <div class="card mb-6">
            <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
              Dados Fiscais Cadastrados
            </h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <span class="text-slate-500">Razão Social</span>
                <p class="text-slate-200">{{ config()!.razaoSocial ?? '—' }}</p>
              </div>
              <div>
                <span class="text-slate-500">CNPJ</span>
                <p class="text-slate-200 font-mono">{{ config()!.cnpjMascarado ?? '—' }}</p>
              </div>
              <div>
                <span class="text-slate-500">Inscrição Municipal</span>
                <p class="text-slate-200">{{ config()!.inscricaoMunicipal ?? '—' }}</p>
              </div>
              <div>
                <span class="text-slate-500">Município / UF</span>
                <p class="text-slate-200">
                  {{ config()!.municipio ?? '—' }}{{ config()!.uf ? ' / ' + config()!.uf : '' }}
                </p>
              </div>
              <div>
                <span class="text-slate-500">Código de Serviço</span>
                <p class="text-slate-200">{{ config()!.codigoServicoMunicipal ?? '—' }}</p>
              </div>
              <div>
                <span class="text-slate-500">Alíquota ISS</span>
                <p class="text-slate-200">
                  {{ config()!.aliquotaIss != null ? config()!.aliquotaIss + '%' : '—' }}
                </p>
              </div>
              <div>
                <span class="text-slate-500">E-mail Fiscal</span>
                <p class="text-slate-200">{{ config()!.emailFiscal ?? '—' }}</p>
              </div>
              @if (config()!.fiscalValidatedAt) {
                <div>
                  <span class="text-slate-500">Habilitado em</span>
                  <p class="text-slate-400 text-xs">
                    {{ config()!.fiscalValidatedAt | date:'dd/MM/yyyy HH:mm' : 'UTC' }}
                  </p>
                </div>
              }
            </div>
          </div>
        } @else if (!carregando()) {
          <div class="card mb-6 text-center py-8">
            <p class="text-sm text-slate-500">Nenhuma configuração fiscal cadastrada.</p>
          </div>
        }

        <button (click)="abrirFormulario()" class="btn-primary text-sm">
          {{ config() ? 'Editar dados fiscais' : 'Cadastrar dados fiscais' }}
        </button>
      }

      <!-- Formulário de edição -->
      @if (editando()) {
        <form [formGroup]="form" (ngSubmit)="salvar()" class="space-y-6">
          <div class="card">
            <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
              Dados Fiscais da Empresa
            </h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">

              <div class="sm:col-span-2">
                <label class="form-label">Razão Social</label>
                <input formControlName="razaoSocial" class="form-input" />
              </div>

              <div>
                <label class="form-label">CNPJ</label>
                <input formControlName="cnpj" class="form-input" placeholder="XX.XXX.XXX/XXXX-XX" />
              </div>

              <div>
                <label class="form-label">Inscrição Municipal</label>
                <input formControlName="inscricaoMunicipal" class="form-input" />
              </div>

              <div>
                <label class="form-label">Código Município IBGE</label>
                <input formControlName="codigoMunicipio" class="form-input" placeholder="Ex: 3550308" />
              </div>

              <div>
                <label class="form-label">Município</label>
                <input formControlName="municipio" class="form-input" />
              </div>

              <div>
                <label class="form-label">UF</label>
                <input formControlName="uf" class="form-input" maxlength="2" placeholder="SP" />
              </div>

              <div>
                <label class="form-label">Código Serviço Municipal</label>
                <input formControlName="codigoServicoMunicipal" class="form-input" placeholder="Ex: 14.01" />
              </div>

              <div>
                <label class="form-label">Item Lista de Serviço</label>
                <input formControlName="itemListaServico" class="form-input" placeholder="Ex: 1.01" />
              </div>

              <div>
                <label class="form-label">Alíquota ISS (%)</label>
                <input formControlName="aliquotaIss" type="number" step="0.01" class="form-input" />
              </div>

              <div>
                <label class="form-label">Regime Tributário</label>
                <input formControlName="regimeTributario" class="form-input" placeholder="Ex: Simples Nacional" />
              </div>

              <div>
                <label class="form-label">E-mail Fiscal</label>
                <input formControlName="emailFiscal" type="email" class="form-input" />
              </div>

              <div>
                <label class="form-label">Ambiente Fiscal</label>
                <select formControlName="ambienteFiscal" class="form-input">
                  <option value="HOMOLOGACAO">Homologação (testes)</option>
                  <option value="PRODUCAO">Produção</option>
                </select>
              </div>

              <div class="sm:col-span-2">
                <label class="flex items-center gap-3 cursor-pointer">
                  <input type="checkbox" formControlName="fiscalEnabled"
                         class="w-4 h-4 accent-petroleum-500" />
                  <span class="text-sm text-slate-300">
                    Habilitar emissão de NFS-e pela oficina
                  </span>
                </label>
                <p class="text-xs text-slate-500 mt-1 ml-7">
                  Ative somente após preencher e validar todos os dados fiscais acima.
                </p>
              </div>

            </div>
          </div>

          @if (erro()) {
            <div class="alert-danger text-sm">{{ erro() }}</div>
          }

          <div class="flex gap-3">
            <button type="submit" [disabled]="salvando()" class="btn-primary text-sm">
              {{ salvando() ? 'Salvando...' : 'Salvar dados fiscais' }}
            </button>
            <button type="button" (click)="cancelar()" class="btn-secondary text-sm">Cancelar</button>
          </div>
        </form>
      }
    </div>
  `,
})
export class TenantFiscalConfigComponent implements OnInit {
  private fiscalSvc = inject(FiscalService);
  private fb = inject(FormBuilder);

  readonly carregando = signal(true);
  readonly editando   = signal(false);
  readonly salvando   = signal(false);
  readonly erro       = signal<string | null>(null);
  readonly config     = signal<TenantFiscalConfigResponse | null>(null);

  readonly form = this.fb.group({
    razaoSocial:            [''],
    cnpj:                   [''],
    inscricaoMunicipal:     [''],
    regimeTributario:       [''],
    codigoMunicipio:        [''],
    municipio:              [''],
    uf:                     [''],
    endereco:               [''],
    numero:                 [''],
    bairro:                 [''],
    cep:                    [''],
    emailFiscal:            [''],
    telefoneFiscal:         [''],
    codigoServicoMunicipal: [''],
    itemListaServico:       ['1.01'],
    aliquotaIss:            [2.00],
    ambienteFiscal:         ['HOMOLOGACAO' as FiscalEnvironment],
    fiscalEnabled:          [false],
  });

  ngOnInit() {
    this.fiscalSvc.getTenantFiscalConfig().subscribe({
      next: c => {
        this.config.set(c);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  abrirFormulario() {
    const c = this.config();
    if (c) {
      this.form.patchValue({
        razaoSocial:            c.razaoSocial ?? '',
        cnpj:                   '',
        inscricaoMunicipal:     c.inscricaoMunicipal ?? '',
        regimeTributario:       c.regimeTributario ?? '',
        codigoMunicipio:        c.codigoMunicipio ?? '',
        municipio:              c.municipio ?? '',
        uf:                     c.uf ?? '',
        emailFiscal:            c.emailFiscal ?? '',
        telefoneFiscal:         c.telefoneFiscal ?? '',
        codigoServicoMunicipal: c.codigoServicoMunicipal ?? '',
        itemListaServico:       c.itemListaServico ?? '1.01',
        aliquotaIss:            c.aliquotaIss ?? 2.00,
        ambienteFiscal:         c.ambienteFiscal ?? 'HOMOLOGACAO',
        fiscalEnabled:          c.fiscalEnabled,
      });
    }
    this.editando.set(true);
  }

  salvar() {
    this.salvando.set(true);
    this.erro.set(null);

    const req: TenantFiscalConfigRequest = {
      razaoSocial:            this.form.value.razaoSocial || undefined,
      cnpj:                   this.form.value.cnpj || undefined,
      inscricaoMunicipal:     this.form.value.inscricaoMunicipal || undefined,
      regimeTributario:       this.form.value.regimeTributario || undefined,
      codigoMunicipio:        this.form.value.codigoMunicipio || undefined,
      municipio:              this.form.value.municipio || undefined,
      uf:                     this.form.value.uf || undefined,
      emailFiscal:            this.form.value.emailFiscal || undefined,
      telefoneFiscal:         this.form.value.telefoneFiscal || undefined,
      codigoServicoMunicipal: this.form.value.codigoServicoMunicipal || undefined,
      itemListaServico:       this.form.value.itemListaServico || undefined,
      aliquotaIss:            this.form.value.aliquotaIss ?? undefined,
      ambienteFiscal:         (this.form.value.ambienteFiscal as any) || undefined,
      fiscalEnabled:          this.form.value.fiscalEnabled ?? false,
    };

    this.fiscalSvc.saveTenantFiscalConfig(req).subscribe({
      next: c => {
        this.config.set(c);
        this.editando.set(false);
        this.salvando.set(false);
      },
      error: err => {
        this.erro.set(err?.error?.message ?? 'Erro ao salvar dados fiscais.');
        this.salvando.set(false);
      },
    });
  }

  cancelar() {
    this.editando.set(false);
    this.erro.set(null);
  }
}
