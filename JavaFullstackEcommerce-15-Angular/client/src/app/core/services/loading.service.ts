import { Injectable } from '@angular/core';
import { NgxSpinnerService } from 'ngx-spinner';

/**
 * LoadingService — manages the visibility of the global loading spinner.
 *
 * Works in conjunction with LoadingInterceptor: the interceptor calls loading()
 * before each HTTP request and idle() when each request completes.
 *
 * Why a counter (loadingReqCount)?
 *   When multiple HTTP requests are in flight simultaneously (e.g. products + brands + types
 *   all load at once on the store page), a simple boolean toggle would hide the spinner
 *   as soon as the FIRST request completes, even while the others are still pending.
 *
 *   The counter tracks how many requests are still in-flight:
 *     - loading() increments the count and shows the spinner
 *     - idle() decrements the count and only hides the spinner when count reaches 0
 *   This ensures the spinner stays visible until ALL concurrent requests finish.
 */
@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  /** Number of HTTP requests currently in-flight. Spinner shows while this > 0. */
  loadingReqCount = 0;

  constructor(private spinnerService: NgxSpinnerService) { }

  /**
   * loading — called before an HTTP request is sent.
   *
   * Increments the in-flight counter and shows the spinner.
   * Multiple concurrent calls stack safely (counter goes to 2, 3, etc.).
   */
  loading() {
    this.loadingReqCount++;
    this.spinnerService.show(); // Trigger the NgxSpinner overlay
  }

  /**
   * idle — called when an HTTP request completes (success or error).
   *
   * Decrements the in-flight counter. Only hides the spinner when the counter
   * reaches zero (all concurrent requests have finished).
   * The floor check (loadingReqCount <= 0) prevents the counter from going negative
   * in case idle() is called more times than loading() (defensive programming).
   */
  idle() {
    this.loadingReqCount--;
    if (this.loadingReqCount <= 0) {
      this.loadingReqCount = 0; // Floor at 0 — prevent negative counts
      this.spinnerService.hide(); // Hide spinner only when all requests are done
    }
  }
}
