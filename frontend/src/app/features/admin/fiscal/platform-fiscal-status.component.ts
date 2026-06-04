import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import {
  FiscalService,
  PlatformFiscalConfigResponse,
  PlatformFiscalConfigRequest,
} from '@core/services/fiscal.service';

@Component({
  selector: 'app-platform-fiscal-status',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-wrapper">
      <div class="page-header">
        <div>
          <h1 class="page-title">Configuração Fiscal da Plataforma</h1>
          <p class="page-subtitle">Dados da RiseCode Studio — prestador nas NFS-e de assinatura SaaS</p>
        </div>
      </div>

      <!-- Status cards -->
      @if (!editando()) {
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div class="card text-center">
            <p class="text-xs text-slate-500 mb-1">Status Fiscal</p>
            <span class="badge" [ngClass]="config()?.readyForProduction ? 'badge-active' : 'badge-danger'">
              {{ config()?.readyForProduction ? 'Completo' : 'Incompleto' }}
            </span>
          </div>
          <div class="card text-center">
            <p class="text-xs text-slate-500 mb-1">Ambiente</p>
            <span class="badge"
              [ngClass]="config()?.ambienteFiscal === 'PRODUCAO' ? 'badge-active' : 'badge-warning'">
              {{ config()?.ambienteFiscal ?? 'Não configurado' }}
            </span>
          </div>
          <div class="card text-center">
            <p class="text-xs text-slate-500 mb-1">Última atualização</p>
            <span class="text-xs text-slate-400">
              {{ config()?.updatedAt ? (config()!.updatedAt | date:'dd/MM/yyyy HH:mm' : 'UTC') : '—' }}
            </span>
          </div>
        </div>

        <!-- Alertas de produção -->
        @if (!config()?.readyForProduction) {
          <div class="mb-6 rounded-lg border border-danger-600/30 bg-danger-600/10 p-4">
            <p class="text-sm font-semibold text-danger-400 mb-2">Emissão de NFS-e bloqueada</p>
            <ul class="text-xs text-slate-400 list-disc list-inside space-y-1">
              @if (!config()?.cnpjMascarado) {
                <li>CNPJ da RiseCode Studio não configurado</li>
              }
              @if (!config()?.inscricaoMunicipal) {
                <li>Inscrição Municipal não configurada</li>
              }
              @if (!config()?.codigoServicoMunicipal) {
                <li>Código de Serviço Municipal não configurado</li>
              }
              @if (!config()?.itemListaServico) {
                <li>Item da Lista de Serviço não configurado</li>
              }
            </ul>
          </div>
        }

        <!-- Dados atuais -->
        @if (config()) {
          <div class="card mb-6">
            <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
              Dados Cadastrados
            </h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <span class="text-slate-500">Razão Social</span>
                <p class="text-slate-200">{{ config()!.razaoSocial }}</p>
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
                <p class="text-slate-200">{{ config()!.aliquotaIss != null ? config()!.aliquotaIss + '%' : '—' }}</p>
              </div>
              <div>
                <span class="text-slate-500">E-mail Fiscal</span>
                <p class="text-slate-200">{{ config()!.emailFiscal ?? '—' }}</p>
              </div>
              <div>
                <span class="text-slate-500">Atualizado por</span>
                <p class="text-slate-400 text-xs">{{ config()!.updatedBy ?? '—' }}</p>
              </div>
            </div>
          </div>
        } @else if (!carregando()) {
          <div class="card mb-6 text-center py-8">
            <p class="text-sm text-slate-500">Nenhuma configuração fiscal cadastrada.</p>
          </div>
        }

        <button (click)="editando.set(true)" class="btn-primary text-sm">
          {{ config() ? 'Editar configuração' : 'Cadastrar configuração fiscal' }}
        </button>
      }

      <!-- Formulário -->
      @if (editando()) {
        <form [formGroup]="form" (ngSubmit)="salvar()" class="space-y-6">
          <div class="card">
            <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">
              Dados da RiseCode Studio
            </h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">

              <div class="sm:col-span-2">
                <label class="form-label">Razão Social *</label>
                <input formControlName="razaoSocial" class="form-input" />
                @if (form.get('razaoSocial')?.invalid && form.get('razaoSocial')?.touched) {
                  <p class="form-error">Obrigatório</p>
                }
              </div>

              <div>
                <label class="form-label">CNPJ *</label>
                <input formControlName="cnpj" class="form-input" placeholder="XX.XXX.XXX/XXXX-XX" />
                @if (form.get('cnpj')?.invalid && form.get('cnpj')?.touched) {
                  <p class="form-error">Obrigatório</p>
                }
              </div>

              <div>
                <label class="form-label">Inscrição Municipal</label>
                <input formControlName="inscricaoMunicipal" class="form-input" />
              </div>

              <div>
                <label class="form-label">Código Município IBGE *</label>
                <input formControlName="codigoMunicipio" class="form-input" placeholder="Ex: 3550308" />
                @if (form.get('codigoMunicipio')?.invalid && form.get('codigoMunicipio')?.touched) {
                  <p class="form-error">Obrigatório</p>
                }
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
                <label class="form-label">Ambiente Fiscal *</label>
                <select formControlName="ambienteFiscal" class="form-input">
                  <option value="HOMOLOGACAO">Homologação (testes)</option>
                  <option value="PRODUCAO">Produção</option>
                </select>
              </div>

            </div>
          </div>

          @if (erro()) {
            <div class="alert-danger text-sm">{{ erro() }}</div>
          }

          <div class="flex gap-3">
            <button type="submit" [disabled]="salvando() || form.invalid" class="btn-primary text-sm">
              {{ salvando() ? 'Salvando...' : 'Salvar configuração' }}
            </button>
            <button type="button" (click)="cancelar()" class="btn-secondary text-sm">Cancelar</button>
          </div>
        </form>
      }
    </div>
  `,
})
export class PlatformFiscalStatusComponent implements OnInit {
  private fiscalSvc = inject(FiscalService);
  private fb = inject(FormBuilder);

  readonly carregando = signal(true);
  readonly editando   = signal(false);
  readonly salvando   = signal(false);
  readonly erro       = signal<string | null>(null);
  readonly config     = signal<PlatformFiscalConfigResponse | null>(null);

  readonly form = this.fb.group({
    razaoSocial:            ['RiseCode Studio', Validators.required],
    cnpj:                   ['', Validators.required],
    inscricaoMunicipal:     [''],
    regimeTributario:       [''],
    codigoMunicipio:        ['3550308', Validators.required],
    municipio:              [''],
    uf:                     ['SP'],
    endereco:               [''],
    numero:                 [''],
    bairro:                 [''],
    cep:                    [''],
    emailFiscal:            ['contato@risecodestudio.com.br'],
    telefoneFiscal:         [''],
    codigoServicoMunicipal: [''],
    itemListaServico:       ['1.01'],
    aliquotaIss:            [2.00],
    ambienteFiscal:         ['HOMOLOGACAO' as const, Validators.required],
  });

  ngOnInit() {
    this.fiscalSvc.getPlatformFiscalConfig().subscribe({
      next: c => {
        this.config.set(c);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);

    const req = this.form.value as PlatformFiscalConfigRequest;
    this.fiscalSvc.savePlatformFiscalConfig(req).subscribe({
      next: c => {
        this.config.set(c);
        this.editando.set(false);
        this.salvando.set(false);
      },
      error: err => {
        this.erro.set(err?.error?.message ?? 'Erro ao salvar configuração fiscal.');
        this.salvando.set(false);
      },
    });
  }

  cancelar() {
    this.editando.set(false);
    this.erro.set(null);
  }
}
