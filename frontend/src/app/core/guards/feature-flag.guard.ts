import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { FeatureFlagService } from '@core/services/feature-flag.service';
import { FeatureName } from '@core/models/feature-flag.model';

/**
 * Guard de Feature Flag — bloqueia rotas de módulos desativados.
 * O usuário é redirecionado para a home com uma mensagem amigável.
 *
 * Uso: { canActivate: [authGuard, featureFlagGuard], data: { feature: 'VEHICLE_MANAGEMENT' } }
 */
export const featureFlagGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const featureFlags = inject(FeatureFlagService);
  const router = inject(Router);

  const feature: FeatureName = route.data['feature'];
  if (!feature) return true;

  if (featureFlags.isActive(feature)) return true;

  return router.createUrlTree(['/'], { queryParams: { modulo_desativado: feature } });
};
