import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { UserAdminService } from '@core/services/user-admin.service';
import { UserResponse, UserRole, ROLE_LABELS } from '@core/models/user.model';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-wrapper">

      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Usuários</h1>
          <p class="page-subtitle">Gerencie os acessos ao sistema</p>
        </div>
        <button class="btn-accent" (click)="abrirModal()">
          <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
          Novo Usuário
        </button>
      </div>

      <!-- Erro global -->
      @if (erro()) {
        <div class="alert-danger mb-6">
          <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current flex-shrink-0"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
          {{ erro() }}
        </div>
      }

      <!-- Tabela -->
      <div class="table-container">
        @if (loading()) {
          <div class="loading-pulse">Carregando usuários...</div>
        } @else if (usuarios().length === 0) {
          <div class="empty-state">
            <span class="text-4xl">👤</span>
            <p class="font-medium text-slate-400">Nenhum usuário cadastrado</p>
          </div>
        } @else {
          <table class="w-full">
            <thead class="table-header">
              <tr>
                <th class="th">Nome</th>
                <th class="th">E-mail</th>
                <th class="th">Perfil</th>
                <th class="th">Status</th>
                <th class="th">Cadastro</th>
                <th class="th text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              @for (u of usuarios(); track u.id) {
                <tr class="tr-hover">
                  <td class="td font-semibold text-slate-100">{{ u.fullName }}</td>
                  <td class="td text-slate-400 text-xs font-mono">{{ u.email }}</td>
                  <td class="td">
                    <span class="badge" [class]="roleBadge(u.role)">{{ roleLabel(u.role) }}</span>
                  </td>
                  <td class="td">
                    <span class="badge" [class]="u.enabled ? 'badge-active' : 'badge-inactive'">
                      {{ u.enabled ? 'Ativo' : 'Inativo' }}
                    </span>
                  </td>
                  <td class="td text-xs text-slate-500">
                    {{ u.createdAt | date:'dd/MM/yyyy' }}
                  </td>
                  <td class="td text-right">
                    <div class="flex items-center justify-end gap-2">
                      <button
                        (click)="toggleStatus(u)"
                        [class]="u.enabled ? 'btn-secondary py-1 px-2.5 text-xs' : 'btn-primary py-1 px-2.5 text-xs'">
                        {{ u.enabled ? 'Desativar' : 'Ativar' }}
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>

      <!-- Modal novo usuário -->
      @if (modalAberto()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center px-4">
          <div class="absolute inset-0 bg-black/70" (click)="fecharModal()"></div>

          <div class="relative z-10 w-full max-w-md card space-y-5">
            <div class="flex items-center justify-between">
              <h2 class="text-lg font-bold text-white">Novo Usuário</h2>
              <button (click)="fecharModal()" class="text-slate-500 hover:text-white transition-colors">
                <svg viewBox="0 0 24 24" class="w-5 h-5 fill-current"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
              </button>
            </div>

            @if (erroModal()) {
              <div class="alert-danger">
                <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current flex-shrink-0"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                {{ erroModal() }}
              </div>
            }

            <form [formGroup]="form" (ngSubmit)="salvar()" class="space-y-4">

              <div>
                <label class="form-label">Nome completo</label>
                <input type="text" formControlName="fullName" class="form-input" placeholder="João da Silva">
                @if (form.get('fullName')?.invalid && form.get('fullName')?.touched) {
                  <p class="form-error">Nome obrigatório</p>
                }
              </div>

              <div>
                <label class="form-label">E-mail</label>
                <input type="email" formControlName="email" class="form-input" placeholder="joao@oficina.com">
                @if (form.get('email')?.invalid && form.get('email')?.touched) {
                  <p class="form-error">E-mail inválido</p>
                }
              </div>

              <div>
                <label class="form-label">Senha provisória</label>
                <input type="password" formControlName="password" class="form-input" placeholder="Mínimo 8 caracteres">
                @if (form.get('password')?.invalid && form.get('password')?.touched) {
                  <p class="form-error">Mínimo 8 caracteres</p>
                }
              </div>

              <div>
                <label class="form-label">Perfil de acesso</label>
                <select formControlName="role" class="form-input">
                  @for (role of roles; track role.value) {
                    <option [value]="role.value">{{ role.label }}</option>
                  }
                </select>
              </div>

              <div class="flex gap-3 pt-2">
                <button type="button" class="btn-secondary flex-1" (click)="fecharModal()">Cancelar</button>
                <button type="submit" class="btn-primary flex-1" [disabled]="salvando() || form.invalid">
                  @if (salvando()) { Salvando... } @else { Criar Usuário }
                </button>
              </div>
            </form>
          </div>
        </div>
      }

    </div>
  `,
})
export class UsuariosComponent implements OnInit {
  private service = inject(UserAdminService);
  private fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly salvando = signal(false);
  readonly modalAberto = signal(false);
  readonly usuarios = signal<UserResponse[]>([]);
  readonly erro = signal<string | null>(null);
  readonly erroModal = signal<string | null>(null);

  readonly roles = [
    { value: 'ROLE_GERENTE' as UserRole,      label: 'Gerente' },
    { value: 'ROLE_MECANICO' as UserRole,     label: 'Mecânico' },
    { value: 'ROLE_RECEPCIONISTA' as UserRole, label: 'Recepcionista' },
  ];

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role:     ['ROLE_MECANICO' as UserRole, Validators.required],
  });

  ngOnInit() { this.carregar(); }

  carregar() {
    this.loading.set(true);
    this.service.listar().subscribe({
      next: lista => { this.usuarios.set(lista); this.loading.set(false); },
      error: () => { this.erro.set('Erro ao carregar usuários.'); this.loading.set(false); },
    });
  }

  abrirModal() { this.form.reset({ role: 'ROLE_MECANICO' }); this.erroModal.set(null); this.modalAberto.set(true); }
  fecharModal() { this.modalAberto.set(false); }

  salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    this.erroModal.set(null);

    const { fullName, email, password, role } = this.form.value;
    this.service.criar({ fullName: fullName!, email: email!, password: password!, role: role as UserRole }).subscribe({
      next: novo => {
        this.usuarios.update(lista => [novo, ...lista]);
        this.fecharModal();
        this.salvando.set(false);
      },
      error: err => {
        this.erroModal.set(err.detail ?? 'Erro ao criar usuário.');
        this.salvando.set(false);
      },
    });
  }

  toggleStatus(u: UserResponse) {
    this.service.alterarStatus(u.id, !u.enabled).subscribe({
      next: atualizado => this.usuarios.update(lista => lista.map(x => x.id === u.id ? atualizado : x)),
      error: () => this.erro.set('Erro ao alterar status.'),
    });
  }

  roleLabel(role: UserRole) { return ROLE_LABELS[role] ?? role; }

  roleBadge(role: UserRole): string {
    const map: Record<UserRole, string> = {
      ROLE_ADMIN: 'badge-danger',
      ROLE_GERENTE: 'badge-warning',
      ROLE_MECANICO: 'badge-info',
      ROLE_RECEPCIONISTA: 'badge-inactive',
    };
    return map[role] ?? 'badge-inactive';
  }
}
