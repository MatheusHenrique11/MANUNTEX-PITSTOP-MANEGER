import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { UserRole } from '@core/models/auth.model';

/**
 * Guard de role — protege rotas que exigem perfis específicos.
 * Uso na rota: { canActivate: [authGuard, roleGuard], data: { roles: ['ROLE_ADMIN'] } }
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const requiredRoles: UserRole[] = route.data['roles'] ?? [];
  if (requiredRoles.length === 0) return true;

  const userRole = auth.role();
  if (userRole && requiredRoles.includes(userRole)) return true;

  return router.createUrlTree(['/403']);
};
