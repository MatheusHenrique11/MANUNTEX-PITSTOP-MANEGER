import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptor de credenciais — o mais crítico de segurança.
 *
 * Garante que `withCredentials: true` seja enviado em TODAS as requisições
 * para o backend, permitindo que os cookies HTTP-Only (access_token e
 * refresh_token) sejam incluídos automaticamente pelo browser.
 *
 * Sem isso, os cookies de autenticação não são enviados em cross-origin requests,
 * mesmo que existam no browser.
 */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const secureReq = req.clone({ withCredentials: true });
  return next(secureReq);
};
