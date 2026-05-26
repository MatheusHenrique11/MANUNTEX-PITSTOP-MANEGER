import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { SubscriptionService } from '@core/services/subscription.service';
import { ACTIVE_STATUSES } from '@core/models/subscription.model';

/**
 * Bloqueia rotas protegidas quando a assinatura do tenant não está ativa.
 *
 * Regras:
 * - ROLE_ADMIN (sem empresa): bypass total.
 * - TRIAL ou ACTIVE: acesso permitido.
 * - Qualquer outro status: redireciona para /billing/pricing.
 */
export const subscriptionGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const sub    = inject(SubscriptionService);
  const router = inject(Router);

  // Admins do sistema não têm empresa/assinatura
  if (auth.isAdmin()) return true;

  return sub.fetchStatus().pipe(
    map(status => {
      if (ACTIVE_STATUSES.includes(status)) return true;
      return router.createUrlTree(['/billing/pricing']);
    })
  );
};
