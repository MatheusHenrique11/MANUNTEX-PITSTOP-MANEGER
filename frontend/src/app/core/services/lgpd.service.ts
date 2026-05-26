import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '@env/environment';
import {
  ConsentRequest,
  ConsentStatusResponse,
  DsarRequest,
  DsarResponse,
  MyDataExport,
} from '@core/models/lgpd.model';

@Injectable({ providedIn: 'root' })
export class LgpdService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/lgpd`;

  private readonly _consentStatus = signal<ConsentStatusResponse | null>(null);

  /** true quando ambos os consentimentos obrigatórios foram aceitos */
  readonly allConsented = computed(() => this._consentStatus()?.allRequired ?? false);

  /** Versões atuais — devem ser idênticas às constantes do backend (LgpdService.java) */
  readonly PRIVACY_POLICY_VERSION = '1.0';
  readonly TERMS_OF_USE_VERSION   = '1.0';

  // ── Consentimento ───────────────────────────────────────────────────────────

  getConsentStatus(): Observable<ConsentStatusResponse> {
    const cached = this._consentStatus();
    if (cached !== null) {
      return of(cached);
    }
    return this.http.get<ConsentStatusResponse>(`${this.apiUrl}/consent/status`).pipe(
      tap(s => this._consentStatus.set(s))
    );
  }

  recordConsent(request: ConsentRequest): Observable<ConsentStatusResponse> {
    return this.http.post<ConsentStatusResponse>(`${this.apiUrl}/consent`, request).pipe(
      tap(s => this._consentStatus.set(s))
    );
  }

  invalidateConsentCache(): void {
    this._consentStatus.set(null);
  }

  // ── Direitos do Titular ─────────────────────────────────────────────────────

  exportMyData(): Observable<MyDataExport> {
    return this.http.get<MyDataExport>(`${this.apiUrl}/my-data`);
  }

  submitDsar(request: DsarRequest): Observable<DsarResponse> {
    return this.http.post<DsarResponse>(`${this.apiUrl}/dsar`, request);
  }

  listMyDsars(): Observable<DsarResponse[]> {
    return this.http.get<DsarResponse[]>(`${this.apiUrl}/dsar`);
  }

  // ── Admin ───────────────────────────────────────────────────────────────────

  listAllDsars(): Observable<DsarResponse[]> {
    return this.http.get<DsarResponse[]>(`${this.apiUrl}/admin/dsars`);
  }

  completeDsar(id: string, responseNotes: string): Observable<DsarResponse> {
    return this.http.put<DsarResponse>(
      `${this.apiUrl}/admin/dsars/${id}/complete`,
      null,
      { params: { responseNotes } }
    );
  }

  anonymizeUser(userId: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/admin/dsars/${userId}/anonymize`, null);
  }
}
