import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentoService } from '@core/services/documento.service';
import { TipoDocumento } from '@core/models/documento.model';

@Component({
  selector: 'app-documento-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="p-8 max-w-xl mx-auto">

      <h1 class="text-2xl font-bold text-gray-900 mb-6">📄 Upload de Documento</h1>

      <div class="card space-y-5">

        <!-- Tipo -->
        <div>
          <label class="form-label">Tipo de Documento</label>
          <select [(ngModel)]="tipo" class="form-input">
            <option value="CRLV">CRLV</option>
            <option value="CNH">CNH</option>
            <option value="SEGURO">Seguro</option>
            <option value="LAUDO_CAUTELAR">Laudo Cautelar</option>
            <option value="NOTA_FISCAL">Nota Fiscal</option>
            <option value="OUTRO">Outro</option>
          </select>
        </div>

        <!-- Veículo ID -->
        <div>
          <label class="form-label">ID do Veículo</label>
          <input type="text" [(ngModel)]="veiculoId" class="form-input"
                 placeholder="UUID do veículo (opcional)">
        </div>

        <!-- Drop zone -->
        <div
          class="border-2 border-dashed rounded-lg p-8 text-center transition-colors"
          [class.border-brand-400]="isDragging"
          [class.bg-brand-50]="isDragging"
          [class.border-gray-300]="!isDragging"
          (dragover)="onDragOver($event)"
          (dragleave)="isDragging = false"
          (drop)="onDrop($event)">

          @if (selectedFile()) {
            <div>
              <p class="text-sm font-medium text-gray-900">{{ selectedFile()!.name }}</p>
              <p class="text-xs text-gray-500 mt-1">{{ formatSize(selectedFile()!.size) }}</p>
              <button (click)="clearFile()" class="mt-2 text-xs text-red-500 hover:text-red-700">
                Remover
              </button>
            </div>
          } @else {
            <div>
              <p class="text-4xl mb-2">📎</p>
              <p class="text-sm font-medium text-gray-700">Arraste um PDF aqui</p>
              <p class="text-xs text-gray-400 mt-1">ou</p>
              <label class="mt-2 inline-block cursor-pointer text-sm text-brand-600 hover:text-brand-800 font-medium underline">
                selecione um arquivo
                <input type="file" accept=".pdf,application/pdf" class="hidden" (change)="onFileSelect($event)">
              </label>
              <p class="text-xs text-gray-400 mt-2">Apenas PDF, máx. 10 MB</p>
            </div>
          }
        </div>

        <!-- Mensagens -->
        @if (error()) {
          <div class="px-3 py-2 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
            {{ error() }}
          </div>
        }
        @if (success()) {
          <div class="px-3 py-2 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
            ✅ Documento enviado com segurança e criptografado no servidor.
          </div>
        }

        <!-- Submit -->
        <button
          (click)="upload()"
          [disabled]="!selectedFile() || loading()"
          class="btn-primary w-full">
          @if (loading()) {
            <span class="animate-spin mr-2">⟳</span> Enviando e criptografando...
          } @else {
            🔒 Enviar com Criptografia
          }
        </button>

      </div>
    </div>
  `,
})
export class DocumentoUploadComponent {
  private documentoService = inject(DocumentoService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal(false);
  readonly selectedFile = signal<File | null>(null);

  tipo: TipoDocumento = 'CRLV';
  veiculoId = '';
  isDragging = false;

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) this.setFile(input.files[0]);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragging = true;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragging = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.setFile(file);
  }

  private setFile(file: File) {
    this.error.set(null);
    this.success.set(false);
    // Validação client-side de extensão (o server valida Magic Numbers)
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      this.error.set('Apenas arquivos PDF são aceitos.');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.error.set('O arquivo excede o limite de 10 MB.');
      return;
    }
    this.selectedFile.set(file);
  }

  clearFile() {
    this.selectedFile.set(null);
    this.error.set(null);
    this.success.set(false);
  }

  upload() {
    const file = this.selectedFile();
    if (!file) return;

    this.loading.set(true);
    this.error.set(null);
    this.success.set(false);

    this.documentoService.upload(file, this.tipo, this.veiculoId || undefined).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        this.selectedFile.set(null);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.detail ?? 'Erro ao enviar o documento.');
      },
    });
  }

  formatSize(bytes: number): string {
    return bytes < 1024 * 1024
      ? `${(bytes / 1024).toFixed(1)} KB`
      : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
