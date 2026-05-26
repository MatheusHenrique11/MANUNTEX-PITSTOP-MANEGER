import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '@env/environment';
import {
  AssinaturaResponse,
  CheckoutRequest,
  CheckoutResponse,
  FaturaNfeResponse,
  SubscriptionPlan,
  SubscriptionStatus,
} from '@core/models/subscription.model';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/billing`;

  private readonly _status = signal<SubscriptionStatus | null>(null);
  readonly status = this._status.asReadonly();

  getAssinatura(): Observable<AssinaturaResponse> {
    return this.http.get<AssinaturaResponse>(`${this.base}/subscription`);
  }

  getFaturas(): Observable<FaturaNfeResponse[]> {
    return this.http.get<FaturaNfeResponse[]>(`${this.base}/invoices`);
  }

  criarCheckout(plano: SubscriptionPlan): Observable<CheckoutResponse> {
    const body: CheckoutRequest = { plano };
    return this.http.post<CheckoutResponse>(`${this.base}/checkout`, body);
  }

  cancelarAssinatura(): Observable<void> {
    return this.http.delete<void>(`${this.base}/subscription`);
  }

  /**
   * Busca e armazena em cache o status da assinatura.
   * Usado pelo subscriptionGuard para não fazer múltiplas requisições.
   */
  fetchStatus(): Observable<SubscriptionStatus> {
    const cached = this._status();
    if (cached !== null) return of(cached);

    return this.http.get<SubscriptionStatus>(`${this.base}/subscription/status`).pipe(
      tap(status => this._status.set(status)),
      catchError(() => {
        this._status.set('SUSPENDED');
        return of('SUSPENDED' as SubscriptionStatus);
      })
    );
  }

  /** Invalida o cache de status (chamar após checkout ou cancelamento). */
  invalidateStatus(): void {
    this._status.set(null);
  }
}
