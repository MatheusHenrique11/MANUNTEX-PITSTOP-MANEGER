import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const statusLabel = statusText(error.status);

      console.error(
        `[HTTP ${error.status} ${statusLabel}] ${req.method} ${req.url}`,
        '\nBody:', error.error,
        '\nHeaders:', Object.fromEntries(
          ['content-type', 'x-request-id'].map(h => [h, error.headers?.get(h)])
        )
      );

      const normalized = {
        status: error.status,
        title: error.error?.title ?? statusLabel,
        detail: error.error?.detail ?? friendlyMessage(error.status),
        fields: error.error?.fields ?? null,
      };

      return throwError(() => normalized);
    })
  );
};

function statusText(status: number): string {
  const map: Record<number, string> = {
    0:   'Connection Refused — backend inacessível',
    400: 'Bad Request',
    401: 'Unauthorized — não autenticado',
    403: 'Forbidden — credenciais inválidas ou sem permissão',
    404: 'Not Found — endpoint não existe',
    422: 'Unprocessable Entity — dados inválidos',
    500: 'Internal Server Error',
    502: 'Bad Gateway',
    503: 'Service Unavailable',
  };
  return map[status] ?? `HTTP ${status}`;
}

function friendlyMessage(status: number): string {
  switch (status) {
    case 0:   return 'Não foi possível conectar ao servidor. Verifique se o backend está online.';
    case 401: return 'Sessão expirada. Faça login novamente.';
    case 403: return 'E-mail ou senha incorretos.';
    case 404: return 'Recurso não encontrado.';
    case 422: return 'Dados inválidos. Verifique o formulário.';
    case 500: return 'Erro interno no servidor. Tente novamente em alguns instantes.';
    default:  return 'Erro inesperado. Tente novamente.';
  }
}
