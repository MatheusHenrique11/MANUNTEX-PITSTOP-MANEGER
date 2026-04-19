import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '@env/environment';
import { FeatureName, FeatureFlagsMap } from '@core/models/feature-flag.model';

/**
 * Carrega e mantém o estado das Feature Flags do backend.
 * Usa Signals para que componentes e guards reativos detectem mudanças imediatamente.
 */
@Injectable({ providedIn: 'root' })
export class FeatureFlagService {
  private readonly apiUrl = `${environment.apiUrl}/features`;

  private readonly _flags = signal<FeatureFlagsMap | null>(null);

  readonly flags = computed(() => this._flags());

  constructor(private http: HttpClient) {}

  load() {
    return this.http.get<FeatureFlagsMap>(this.apiUrl).pipe(
      tap(flags => this._flags.set(flags))
    );
  }

  isActive(feature: FeatureName): boolean {
    return this._flags()?.[feature]?.active ?? false;
  }

  isActiveSignal(feature: FeatureName) {
    return computed(() => this._flags()?.[feature]?.active ?? false);
  }
}
