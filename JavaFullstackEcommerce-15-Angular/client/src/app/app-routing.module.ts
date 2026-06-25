import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { NotFoundComponent } from './core/not-found/not-found.component';
import { ServerErrorComponent } from './core/server-error/server-error.component';

/**
 * AppRoutingModule — top-level route configuration for the SportsCenter application.
 *
 * All feature routes use lazy loading (loadChildren with dynamic import()):
 *   - The browser downloads a separate JS chunk for each module only when navigating to it
 *   - This keeps the initial bundle small (only home + shell code loads on first visit)
 *   - Each feature module has its own routing module for child routes within that section
 *
 * Route priority:
 *   Routes are matched in order — '' (home) must come before '**' (wildcard catch-all).
 *   The '**' wildcard redirects any unknown path back to '' (home), matching no breadcrumb.
 *
 * Route guard:
 *   The /checkout route has canActivate: [canActivate] guard (from auth.guard.ts).
 *   It's not shown here because the guard is applied in the checkout module's own routing file
 *   to keep each feature self-contained.
 *
 * data.breadcrumb:
 *   The xng-breadcrumb library reads route data to generate the breadcrumb trail
 *   (e.g. "Home / Store / Product Name").
 */
const routes: Routes = [
  // Root route — HomeComponent is eagerly loaded (included in the main bundle via HomeModule)
  { path: '', component: HomeComponent, data: { breadcrumb: 'Home' } },

  // Store module — lazy loaded: downloads store.module.js only on /store navigation
  { path: 'store', loadChildren: () => import('./store/store.module').then(m => m.StoreModule) },

  // Basket module — lazy loaded: basket.module.js downloaded on /basket navigation
  { path: 'basket', loadChildren: () => import('./basket/basket.module').then(m => m.BasketModule) },

  // Account module — lazy loaded: account.module.js downloaded on /account navigation
  // Contains login and register sub-routes (defined in account-routing.module.ts)
  { path: 'account', loadChildren: () => import('./account/account.module').then(m => m.AccountModule) },

  // Checkout module — lazy loaded with canActivate guard in checkout-routing.module.ts
  // Users must be authenticated (JWT in localStorage) to access /checkout
  { path: 'checkout', loadChildren: () => import('./checkout/checkout.module').then(m => m.CheckoutModule) },

  // Error pages — eagerly loaded (part of CoreModule, always available)
  { path: 'not-found', component: NotFoundComponent },    // Shown on 404 errors (ErrorInterceptor)
  { path: 'server-error', component: ServerErrorComponent }, // Shown on 500 errors

  // Catch-all wildcard — redirect any unknown path to home rather than showing a blank page
  { path: '**', redirectTo: '', pathMatch: 'full' }
];

/**
 * AppRoutingModule — wires the routes into the Angular router.
 *
 * RouterModule.forRoot(routes) initialises the router at the app root level.
 * (Feature modules use RouterModule.forChild() for their own child routes.)
 * Exporting RouterModule makes <router-outlet> and routerLink available
 * to all components in AppModule.
 */
@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
