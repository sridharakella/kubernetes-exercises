import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable, catchError } from 'rxjs';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

/**
 * ErrorInterceptor — global HTTP error handler for all Angular HTTP requests.
 *
 * Implements Angular's HttpInterceptor interface. Registered in AppModule providers
 * with multi:true so it runs alongside other interceptors (LoadingInterceptor).
 *
 * Why a centralised interceptor?
 *   Without it, every service call would need its own error handling, leading to
 *   duplicated code across 10+ services. One interceptor handles it all.
 *
 * Current error handling:
 *   404 Not Found     → Toastr error notification + navigate to /not-found
 *   500 Server Error  → Toastr error notification + navigate to /server-error
 *   Other errors      → re-thrown to individual subscribers (they can handle specifically)
 *
 * The Spring Boot CustomExceptionHandler returns 404 for ProductNotFoundException
 * and 500 for unexpected server errors, so these two cases cover most scenarios.
 */
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  constructor(
    private router: Router,       // For navigation to error pages
    private toastr: ToastrService // For toast notification display
  ) {}

  /**
   * intercept — wraps every outgoing HTTP request with error catching.
   *
   * Uses RxJS catchError operator to intercept HTTP error responses.
   * catchError receives the HttpErrorResponse when the server returns a 4xx or 5xx.
   *
   * Note: `throw error` re-throws the error after handling navigation/toastr,
   * so any individual service subscribers can still react if needed.
   *
   * @param request the outgoing HTTP request
   * @param next    the next handler in the interceptor chain
   * @returns Observable of the HTTP event stream with error handling applied
   */
  intercept(
    request: HttpRequest<unknown>,
    next: HttpHandler
  ): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error) => {
        if (error.status === 404) {
          // Resource not found — show toast and navigate to the not-found page
          this.toastr.error('404 error happened');
          this.router.navigate(['/not-found']);
        } else if (error.status === 500) {
          // Server error — show toast and navigate to the server-error page
          this.toastr.error('500 error happened');
          this.router.navigate(['/server-error']);
        }
        // Re-throw the error so individual subscribers can also react if needed
        // (e.g. LoginComponent showing "Invalid username or password" for 401)
        throw error;
      })
    );
  }
}
