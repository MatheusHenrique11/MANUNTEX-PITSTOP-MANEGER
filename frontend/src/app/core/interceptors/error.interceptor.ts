import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/**
 * Normaliza erros HTTP para o formato ProblemDetail (RFC 7807) que o backend retorna.
 * Evita que mensagens de erro brutas do servidor apareçam na UI.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const normalized = {
        status: error.status,
        title: error.error?.title ?? 'Erro inesperado',
        detail: error.error?.detail ?? 'Tente novamente. Se o problema persistir, contate o suporte.',
        fields: error.error?.fields ?? null,
      };
      return throwError(() => normalized);
    })
  );
};
