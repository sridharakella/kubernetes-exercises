import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { AccountService } from 'src/app/account/account.service';

/**
 * canActivate — Angular functional route guard for protecting authenticated routes.
 *
 * Applied to the /checkout route in app-routing.module.ts:
 *   { path: 'checkout', loadChildren: ..., canActivate: [canActivate] }
 *
 * Uses the newer Angular 15+ functional guard syntax (CanActivateFn) instead of
 * the class-based CanActivate interface. Dependencies are injected with inject()
 * rather than constructor injection.
 *
 * Guard logic:
 *   - If the user HAS a JWT in localStorage (isAuthenticated = true) → allow navigation
 *   - If the user is NOT authenticated → save the attempted URL and redirect to login
 *     The returnUrl query param is passed so the LoginComponent can redirect back
 *     to the original destination after successful login.
 *
 * Return values of CanActivateFn:
 *   true          → allow navigation to the route
 *   UrlTree       → redirect to a different route (preferred over router.navigate in guards)
 *   false         → block navigation (no redirect — generally avoid this)
 *
 * @param route the activated route snapshot (not used here, but required by the type)
 * @param state contains state.url — the URL the user was trying to navigate to
 * @returns true to allow, or a UrlTree redirecting to the login page
 */
export const canActivate: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  // Inject dependencies using Angular's functional injection API
  const accountService = inject(AccountService);
  const router = inject(Router);

  if (accountService.isAuthenticated()) {
    return true; // User has a token — allow access to the protected route
  } else {
    // Store the attempted URL so LoginComponent can redirect back after login
    accountService.redirectUrl = state.url;

    // Return a UrlTree instead of calling router.navigate() directly.
    // UrlTree is the recommended approach in guards — it cancels the current navigation
    // and starts a new one to /account/login in a single atomic operation.
    return router.createUrlTree(['/account/login'], {
      queryParams: { returnUrl: state.url } // e.g. ?returnUrl=%2Fcheckout
    });
  }
};
