import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { LgpdService } from '@core/services/lgpd.service';

/**
 * Garante que o usuário aceitou a Política de Privacidade e os Termos de Uso
 * antes de acessar qualquer rota protegida. (Art. 8 LGPD — consentimento livre,
 * informado e inequívoco)
 *
 * Se ainda não consentiu, redireciona para /consent?returnUrl=<rota-original>.
 */
export const consentGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const lgpd   = inject(LgpdService);
  const router = inject(Router);

  return lgpd.getConsentStatus().pipe(
    map(status => {
      if (status.allRequired) return true;
      return router.createUrlTree(['/consent'], {
        queryParams: { returnUrl: state.url },
      });
    }),
    catchError(() => of(true)) // falha silenciosa: não bloqueia por erro de rede
  );
};
