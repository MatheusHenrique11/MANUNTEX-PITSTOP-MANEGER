import { ApplicationConfig, APP_INITIALIZER } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { credentialsInterceptor } from '@core/interceptors/credentials.interceptor';
import { authRefreshInterceptor } from '@core/interceptors/auth-refresh.interceptor';
import { errorInterceptor } from '@core/interceptors/error.interceptor';
import { FeatureFlagService } from '@core/services/feature-flag.service';
import { catchError, EMPTY } from 'rxjs';

/**
 * Inicializa as Feature Flags antes de renderizar qualquer rota.
 * Garante que featureFlagGuard sempre tem dados para decidir.
 */
function initFeatureFlags(service: FeatureFlagService) {
  return () => service.load().pipe(catchError(() => EMPTY));
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),

    provideHttpClient(
      withInterceptors([
        credentialsInterceptor,    // 1º: sempre envia cookies
        authRefreshInterceptor,    // 2º: renova token em 401
        errorInterceptor,          // 3º: normaliza erros
      ])
    ),

    {
      provide: APP_INITIALIZER,
      useFactory: initFeatureFlags,
      deps: [FeatureFlagService],
      multi: true,
    },
  ],
};
