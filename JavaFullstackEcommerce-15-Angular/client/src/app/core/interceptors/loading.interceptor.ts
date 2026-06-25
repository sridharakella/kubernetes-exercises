import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable, delay, finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

/**
 * LoadingInterceptor — shows and hides the NgxSpinner loading overlay on every HTTP request.
 *
 * Registered in AppModule providers with multi:true alongside ErrorInterceptor.
 * Interceptors run in the order they are declared in the providers array.
 *
 * How it works:
 *   1. Before the request is sent:   call loadingService.loading() → spinner.show()
 *   2. After the response arrives:   finalize() calls loadingService.idle() → spinner.hide()
 *
 * finalize() is used (not tap) because it runs on BOTH success AND error completion,
 * ensuring the spinner always hides even if the request fails.
 *
 * LoadingService tracks a request counter (loadingReqCount) to support concurrent requests:
 *   - spinner.show() is called once per request
 *   - spinner.hide() is only called when ALL pending requests complete (count reaches 0)
 *   This prevents the spinner from flickering off mid-flight when multiple requests overlap.
 *
 * The delay(1000) adds an artificial 1-second pause before the response is emitted.
 * This makes the spinner visible even for very fast local API calls.
 * IMPORTANT: Remove delay(1000) in production to avoid degrading real-world performance.
 */
@Injectable()
export class LoadingInterceptor implements HttpInterceptor {
  constructor(private loadingService: LoadingService) {}

  /**
   * intercept — wraps every HTTP request with loading state management.
   *
   * @param request the outgoing HTTP request (not modified here)
   * @param next    passes the request to the next handler in the chain
   * @returns Observable of the HTTP event stream with loading lifecycle hooks
   */
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    this.loadingService.loading(); // Show spinner before the request is sent

    return next.handle(request).pipe(
      delay(1000), // DEVELOPMENT ONLY: artificial delay to show the spinner for fast requests
      finalize(() => this.loadingService.idle()) // Always hide spinner when request completes
    );
  }
}
