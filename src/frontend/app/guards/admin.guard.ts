import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.getToken()) {
    return router.createUrlTree(['/login']);
  }

  if (authService.getProfile()?.role === 'ADMIN') {
    return true;
  }

  return authService.loadProfile().pipe(
    map((profile) => (profile?.role === 'ADMIN' ? true : router.createUrlTree(['/']))),
  );
};