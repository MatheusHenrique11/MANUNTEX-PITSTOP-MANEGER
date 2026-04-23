import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { FeatureFlagService } from '@core/services/feature-flag.service';
import { AuthService } from '@core/services/auth.service';
import { FeatureName, DISABLED_MODULE_PARAM } from '@core/models/feature-flag.model';

export const featureFlagGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const featureFlags = inject(FeatureFlagService);
  const auth = inject(AuthService);
  const router = inject(Router);

  const feature: FeatureName = route.data['feature'];
  if (!feature) return true;
  if (auth.isAdmin()) return true;
  if (featureFlags.isActive(feature)) return true;

  return router.createUrlTree(['/dashboard'], { queryParams: { [DISABLED_MODULE_PARAM]: feature } });
};
