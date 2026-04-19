import { Component, inject, signal, OnInit, OnDestroy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentoService } from '@core/services/documento.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * Exibe um PDF descriptografado pelo backend em um iframe seguro.
 *
 * O fluxo: backend descriptografa → envia bytes → component cria Blob URL temporária
 * → iframe renderiza → ao destruir o component, a Blob URL é revogada da memória.
 *
 * O PDF nunca é persistido no storage do browser (sem cache, sem Service Worker).
 */
@Component({
  selector: 'app-documento-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-8">
      <h1 class="text-xl font-bold text-gray-900 mb-4">📄 Visualizar Documento</h1>

      @if (loading()) {
        <div class="card py-16 text-center text-gray-400">
          <span class="animate-spin inline-block mr-2">⟳</span> Descriptografando documento...
        </div>
      } @else if (error()) {
        <div class="card py-8 text-center text-red-600">{{ error() }}</div>
      } @else if (pdfUrl()) {
        <div class="card p-0 overflow-hidden">
          <!-- sandbox impede scripts no PDF e abre links externamente -->
          <iframe
            [src]="pdfUrl()!"
            class="w-full"
            style="height: 85vh;"
            sandbox="allow-same-origin"
            title="Visualizador de documento">
          </iframe>
        </div>
        <p class="mt-2 text-xs text-gray-400">
          🔒 Este documento é exibido de forma segura e não fica armazenado localmente.
        </p>
      }
    </div>
  `,
})
export class DocumentoViewerComponent implements OnInit, OnDestroy {
  @Input() id!: string;

  private documentoService = inject(DocumentoService);
  private sanitizer = inject(DomSanitizer);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly pdfUrl = signal<SafeResourceUrl | null>(null);

  private blobUrl: string | null = null;

  ngOnInit() {
    this.documentoService.getContent(this.id).subscribe({
      next: (blob: Blob) => {
        this.blobUrl = this.documentoService.createBlobUrl(blob);
        // bypassSecurityTrustResourceUrl é seguro aqui — URL é criada localmente como blob:
        this.pdfUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.blobUrl));
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.detail ?? 'Não foi possível carregar o documento.');
      },
    });
  }

  ngOnDestroy() {
    // Libera a Blob URL da memória ao sair da tela
    if (this.blobUrl) {
      this.documentoService.revokeBlobUrl(this.blobUrl);
    }
  }
}
