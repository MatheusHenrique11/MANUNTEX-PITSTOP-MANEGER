import { Component, inject, signal, OnInit, Input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { VeiculoService } from '@core/services/veiculo.service';
import { ClienteService } from '@core/services/cliente.service';
import { Cliente } from '@core/models/cliente.model';

const PLACA_ANTIGA   = /^[A-Za-z]{3}[0-9]{4}$/;
const PLACA_MERCOSUL = /^[A-Za-z]{3}[0-9][A-Za-z][0-9]{2}$/;

function placaValidator(control: { value: string }) {
  if (!control.value) return null;
  const v = control.value.replace(/[-\s]/g, '').toUpperCase();
  return PLACA_ANTIGA.test(v) || PLACA_MERCOSUL.test(v) ? null : { placa: true };
}

@Component({
  selector: 'app-veiculo-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="p-8 max-w-2xl mx-auto">
      <h1 class="text-2xl font-bold text-gray-900 mb-6">
        {{ id ? 'Editar' : 'Novo' }} Veículo
      </h1>

      <form [formGroup]="form" (ngSubmit)="submit()" class="card space-y-5">

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="form-label">Placa *</label>
            <input formControlName="placa" class="form-input font-mono uppercase"
                   placeholder="ABC1234 ou ABC1D23" maxlength="8">
            @if (hasError('placa')) {
              <p class="form-error">Placa inválida (Antigo ou Mercosul)</p>
            }
          </div>
          <div>
            <label class="form-label">Cor</label>
            <input formControlName="cor" class="form-input" placeholder="Prata">
          </div>
        </div>

        <div>
          <label class="form-label">Chassi * <span class="text-gray-400 text-xs">(17 caracteres)</span></label>
          <input formControlName="chassi" class="form-input font-mono uppercase"
                 placeholder="9BWZZZ377VT004251" maxlength="17">
          @if (hasError('chassi')) {
            <p class="form-error">Chassi inválido (17 caracteres, padrão ISO 3779)</p>
          }
        </div>

        <div>
          <label class="form-label">RENAVAM * <span class="text-gray-400 text-xs">(9 ou 11 dígitos)</span></label>
          <input formControlName="renavam" class="form-input font-mono"
                 placeholder="00258665590" maxlength="11">
          @if (hasError('renavam')) {
            <p class="form-error">RENAVAM inválido</p>
          }
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="form-label">Marca *</label>
            <input formControlName="marca" class="form-input" placeholder="Volkswagen">
          </div>
          <div>
            <label class="form-label">Modelo *</label>
            <input formControlName="modelo" class="form-input" placeholder="Gol">
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="form-label">Ano Fabricação *</label>
            <input formControlName="anoFabricacao" type="number" class="form-input"
                   min="1900" max="2100">
          </div>
          <div>
            <label class="form-label">Ano Modelo *</label>
            <input formControlName="anoModelo" type="number" class="form-input"
                   min="1900" max="2100">
          </div>
        </div>

        <!-- Cliente autocomplete -->
        <div class="relative">
          <label class="form-label">Cliente *</label>
          <input
            #clienteInput
            type="text"
            class="form-input"
            [class.border-red-400]="form.get('clienteId')?.invalid && form.get('clienteId')?.touched"
            [value]="clienteNomeSelecionado()"
            (input)="onClienteInput(clienteInput.value)"
            (blur)="onClienteBlur()"
            placeholder="Digite o nome ou CPF/CNPJ do cliente"
            [disabled]="!!id"
          >
          @if (clienteSugestoes().length > 0) {
            <ul class="absolute z-20 w-full bg-white border border-gray-200 rounded-lg shadow-lg mt-1 max-h-52 overflow-y-auto">
              @for (c of clienteSugestoes(); track c.id) {
                <li
                  class="px-4 py-2 hover:bg-blue-50 cursor-pointer text-sm"
                  (mousedown)="selecionarCliente(c)"
                >
                  <span class="font-medium">{{ c.nome }}</span>
                  <span class="ml-2 text-gray-400 text-xs">{{ c.cpfCnpj }}</span>
                </li>
              }
            </ul>
          }
          @if (form.get('clienteId')?.invalid && form.get('clienteId')?.touched) {
            <p class="form-error">Selecione um cliente da lista</p>
          }
        </div>

        @if (error()) {
          <div class="px-3 py-2 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
            {{ error() }}
          </div>
        }

        <div class="flex gap-3 justify-end pt-2">
          <button type="button" (click)="router.navigate(['/veiculos'])" class="btn-secondary">
            Cancelar
          </button>
          <button type="submit" [disabled]="form.invalid || loading()" class="btn-primary">
            {{ loading() ? 'Salvando...' : 'Salvar Veículo' }}
          </button>
        </div>

      </form>
    </div>
  `,
})
export class VeiculoFormComponent implements OnInit {
  @Input() id?: string;

  private fb = inject(FormBuilder);
  readonly router = inject(Router);
  private veiculoService = inject(VeiculoService);
  private clienteService = inject(ClienteService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly clienteSugestoes = signal<Cliente[]>([]);
  readonly clienteNomeSelecionado = signal('');

  private readonly searchSubject = new Subject<string>();

  readonly form = this.fb.group({
    placa:         ['', [Validators.required, placaValidator]],
    chassi:        ['', [Validators.required, Validators.minLength(17), Validators.maxLength(17)]],
    renavam:       ['', [Validators.required, Validators.pattern(/^\d{9,11}$/)]],
    marca:         ['', [Validators.required, Validators.maxLength(60)]],
    modelo:        ['', [Validators.required, Validators.maxLength(80)]],
    anoFabricacao: [new Date().getFullYear(), [Validators.required, Validators.min(1900), Validators.max(2100)]],
    anoModelo:     [new Date().getFullYear(), [Validators.required, Validators.min(1900), Validators.max(2100)]],
    cor:           [''],
    clienteId:     ['', Validators.required],
  });

  ngOnInit() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => q.length >= 2 ? this.clienteService.listar(q, 0, 10) : [])
    ).subscribe(page => {
      this.clienteSugestoes.set('content' in page ? (page as any).content : []);
    });

    if (this.id) {
      this.veiculoService.buscarPorId(this.id).subscribe(v => {
        this.form.patchValue(v);
        this.form.get('chassi')?.disable();
        this.form.get('renavam')?.disable();
      });
    }
  }

  onClienteInput(value: string) {
    this.clienteNomeSelecionado.set(value);
    this.form.get('clienteId')?.setValue('');
    this.searchSubject.next(value);
  }

  selecionarCliente(cliente: Cliente) {
    this.form.get('clienteId')?.setValue(cliente.id);
    this.clienteNomeSelecionado.set(cliente.nome);
    this.clienteSugestoes.set([]);
  }

  onClienteBlur() {
    setTimeout(() => this.clienteSugestoes.set([]), 150);
  }

  submit() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();
    this.veiculoService.criar(value as any).subscribe({
      next: (v) => this.router.navigate(['/veiculos', v.id]),
      error: (err) => {
        this.loading.set(false);
        const fieldErrors = err.fields ? Object.values(err.fields).join(', ') : '';
        this.error.set(fieldErrors || err.detail || 'Erro ao salvar veículo.');
      },
    });
  }

  hasError(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && c.touched);
  }
}
