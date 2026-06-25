import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CoreModule } from './core/core.module';
import { HomeModule } from './home/home.module';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';
import { LoadingInterceptor } from './core/interceptors/loading.interceptor';
import { NgxSpinnerModule } from 'ngx-spinner';

/**
 * AppModule — the root Angular module; the entry point of the entire application.
 *
 * Angular bootstraps the app by loading AppModule, which:
 *   1. Declares the root AppComponent (shell with NavBar + <router-outlet>)
 *   2. Imports shared framework modules (BrowserModule, HttpClientModule, etc.)
 *   3. Imports CoreModule (NavBar, Guards, error pages — loaded once, not lazy)
 *   4. Imports HomeModule (landing page — eagerly loaded with the app)
 *   5. Registers HTTP interceptors for error handling and loading spinner
 *   6. Registers the NgxSpinner with its animation type
 *
 * Feature modules (store, basket, account, checkout) are NOT imported here —
 * they are lazy-loaded via AppRoutingModule when the user navigates to their routes.
 * This keeps the initial bundle size small.
 *
 * @NgModule decorator marks this class as an Angular module.
 */
@NgModule({
  declarations: [
    AppComponent     // The root shell component — hosts <app-nav-bar> and <router-outlet>
  ],
  imports: [
    BrowserModule,            // Core browser support (ngIf, ngFor, DOM rendering)
    AppRoutingModule,         // Top-level lazy routes (store, basket, account, checkout)
    BrowserAnimationsModule,  // Required by Angular Material and NgxSpinner for animations
    HttpClientModule,         // Enables Angular's HttpClient for all HTTP requests
    CoreModule,               // NavBar, error pages, Guards — eagerly loaded once
    HomeModule,               // Landing/home page — eagerly loaded with the app shell

    // NgxSpinner: initialised once with the animation type used across the app.
    // The spinner is shown/hidden by LoadingService (called from LoadingInterceptor).
    NgxSpinnerModule.forRoot({ type: 'square-jelly-box' })
  ],
  providers: [
    // Register ErrorInterceptor — handles 404 and 500 HTTP errors globally.
    // multi: true means Angular keeps all interceptors (doesn't replace previous ones).
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    // Register LoadingInterceptor — shows/hides the spinner on every HTTP request.
    // Declared after ErrorInterceptor; Angular runs interceptors in declaration order.
    {
      provide: HTTP_INTERCEPTORS,
      useClass: LoadingInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent] // Tells Angular which component to mount in index.html's <app-root>
})
export class AppModule { }
