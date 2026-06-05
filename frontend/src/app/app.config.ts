import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { credentialsInterceptor } from '@core/interceptors/credentials.interceptor';
import { authRefreshInterceptor } from '@core/interceptors/auth-refresh.interceptor';
import { errorInterceptor } from '@core/interceptors/error.interceptor';

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
  ],
};
