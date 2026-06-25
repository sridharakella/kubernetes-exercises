import { Injectable } from '@angular/core';
import { BehaviorSubject, map } from 'rxjs';
import { User } from '../shared/models/user';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';

/**
 * AccountService — manages authentication state and JWT token lifecycle.
 *
 * Responsibilities:
 *   - Login: POST credentials → receive JWT → store in localStorage → emit User to subscribers
 *   - Logout: remove JWT from localStorage → emit null → redirect home
 *   - Session restore: on app load, read JWT from localStorage → call /auth/user to verify and restore user
 *   - Auth guard support: isAuthenticated() checks if a JWT exists in localStorage
 *   - Redirect: stores the attempted URL before redirecting to login (for post-login redirect)
 *
 * Why BehaviorSubject?
 *   NavBar subscribes to currentUser$ to show/hide login and display the username.
 *   BehaviorSubject emits the current value immediately on subscribe, so components
 *   always get the latest user state even if they subscribe after login happened.
 */
@Injectable({
  providedIn: 'root'
})
export class AccountService {
  /** Stores the URL the user tried to access before being redirected to login.
   *  AuthGuard sets this; LoginComponent reads it to redirect after successful login. */
  redirectUrl: string | null = null;

  /** Base URL for the Spring Boot auth endpoints */
  apiUrl = 'http://localhost:8080/auth';

  // BehaviorSubject holding the current user (null = not logged in)
  private currentUserSource = new BehaviorSubject<User | null>(null);
  // Public Observable for components to subscribe to user state changes
  currentUser$ = this.currentUserSource.asObservable();

  constructor(private http: HttpClient, private router: Router) { }

  /**
   * isAuthenticated — checks synchronously whether the user has a JWT stored.
   *
   * The double-bang (!!) converts the string (or null) to a boolean.
   * Used by AuthGuard and other places that need a synchronous yes/no answer.
   *
   * Note: this only checks presence, not validity. An expired token still returns true.
   * The server will return 401, which ErrorInterceptor handles by redirecting to login.
   *
   * @returns true if a JWT exists in localStorage, false otherwise
   */
  isAuthenticated(): boolean {
    const token = localStorage.getItem('token');
    return !!token; // !!null = false, !!"token_string" = true
  }

  /**
   * loadUser — restores the user session from a stored JWT on app startup.
   *
   * Called from AppComponent.ngOnInit(). If a token exists in localStorage, it sends the
   * token to /auth/user to get the full UserDetails back, then emits the user on the
   * BehaviorSubject so the NavBar and other components show the user as logged in.
   *
   * This is necessary because BehaviorSubject is in-memory only — it resets to null on
   * every page refresh. Without loadUser(), the user would appear logged out after refresh
   * even though their JWT is still in localStorage.
   */
  loadUser() {
    const token = localStorage.getItem('token');
    if (token) {
      // Manually attach the JWT — the LoadingInterceptor doesn't add auth headers automatically
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`
      });

      this.http
        .get<User>(`${this.apiUrl}/user`, { headers })
        .subscribe({
          next: (user) => {
            this.currentUserSource.next(user); // Restore user state in memory
          },
          error: (error) => {
            // Token is invalid or expired — log but don't crash (user stays logged out)
            console.error('Error decoding JWT token:', token);
          }
        });
    }
  }

  /**
   * login — sends credentials to /auth/login and stores the returned JWT.
   *
   * Returns an Observable (not a subscription) so the LoginComponent can chain .subscribe()
   * to handle success/error in the UI (show errors, redirect on success).
   *
   * The pipe(map(...)) transforms the HTTP response before it reaches the subscriber:
   *   1. Stores the JWT in localStorage (persists across refreshes)
   *   2. Emits the User on the BehaviorSubject (updates NavBar, etc.)
   *
   * @param values { username: string, password: string } from the login form
   * @returns Observable<void> — subscribers get undefined on success (side effects are in map)
   */
  login(values: any) {
    return this.http.post<User>(this.apiUrl + '/login', values).pipe(
      map(user => {
        localStorage.setItem('token', user.token); // Persist JWT for session restore
        this.currentUserSource.next(user);          // Notify all subscribers of new user
      })
    );
  }

  /**
   * register — sends registration data to /auth/register.
   *
   * Note: the Spring Boot backend currently has no /register endpoint.
   * This method is stubbed for future implementation. If called, it will
   * receive a 404 from the server, which ErrorInterceptor will handle.
   *
   * @param values { username: string, password: string } from the register form
   * @returns Observable<void>
   */
  register(values: any) {
    return this.http.post<User>(this.apiUrl + '/register', values).pipe(
      map(user => {
        localStorage.setItem('token', user.token);
        this.currentUserSource.next(user);
      })
    );
  }

  /**
   * logout — clears the JWT, resets user state, and redirects to home.
   *
   * Removes the JWT from localStorage so isAuthenticated() returns false.
   * Emits null on the BehaviorSubject so NavBar switches to the login link.
   * Navigates to '/' to prevent the user staying on a now-protected page.
   */
  logout() {
    localStorage.removeItem('token');   // Clear JWT — isAuthenticated() will return false
    this.currentUserSource.next(null);  // Notify all subscribers (NavBar hides username)
    this.router.navigateByUrl('/');     // Redirect to home page
  }
}
