package com.retroai.controller;

import com.retroai.dto.AuthDtos;
import com.retroai.security.CurrentUser;
import com.retroai.security.JwtAuthFilter;
import com.retroai.service.AtlassianOAuthService;
import com.retroai.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AtlassianOAuthService atlassianOAuthService;
    private final String frontendSuccessUrl;
    private final String cookieDomain;
    private final boolean cookieSecure;

    public AuthController(AuthService authService,
                          AtlassianOAuthService atlassianOAuthService,
                          @Value("${atlassian.oauth.frontend-success-url}") String frontendSuccessUrl,
                          @Value("${app.auth.cookie-domain:}") String cookieDomain,
                          @Value("${app.auth.cookie-secure:true}") boolean cookieSecure) {
        this.authService = authService;
        this.atlassianOAuthService = atlassianOAuthService;
        this.frontendSuccessUrl = frontendSuccessUrl;
        this.cookieDomain = cookieDomain;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.UserResponse register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me() {
        return authService.me(CurrentUser.id());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Clear the JWT cookie (browser keeps it otherwise)
        Cookie clear = new Cookie(JwtAuthFilter.COOKIE_NAME, "");
        clear.setPath("/");
        clear.setMaxAge(0);
        if (cookieDomain != null && !cookieDomain.isBlank()) clear.setDomain(cookieDomain);
        clear.setSecure(cookieSecure);
        clear.setHttpOnly(false);
        response.addCookie(clear);
        return ResponseEntity.noContent().build();
    }

    /**
     * Public probe so the frontend can decide whether to render the
     * "Atlassian ile Giriş Yap" button.
     */
    @GetMapping("/atlassian/status")
    public Map<String, Boolean> atlassianStatus() {
        return Map.of("configured", atlassianOAuthService.isConfigured());
    }

    /**
     * Step 1 of Atlassian OAuth 3LO: 302-redirect the user to Atlassian's
     * consent page.
     */
    @GetMapping("/atlassian")
    public void atlassianRedirect(HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        String url = atlassianOAuthService.buildAuthorizeUrl(state);
        response.sendRedirect(url);
    }

    /**
     * Step 2: Atlassian redirects here with code. Backend exchanges the code
     * for a token, fetches the user profile, auto-creates the local user,
     * stores the Atlassian session for downstream Jira API access, and
     * forwards the user to the frontend board page.
     *
     * Token is delivered to the frontend in three places (defense in depth):
     *  1. {@code retroai_token} cookie (HttpOnly=false so SPAs can read it)
     *  2. URL fragment ({@code #token=...&userId=...&email=...}) — preferred
     *     for SPAs; the fragment never leaves the browser
     *  3. URL query string — fallback for non-SPA frontends
     */
    @GetMapping("/atlassian/callback")
    public void atlassianCallback(@RequestParam(value = "code", required = false) String code,
                                  @RequestParam(value = "state", required = false) String state,
                                  @RequestParam(value = "error", required = false) String error,
                                  HttpServletResponse response) throws IOException {
        if (error != null) {
            response.sendRedirect(frontendSuccessUrl + "?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8));
            return;
        }
        AuthDtos.LoginResponse login;
        try {
            login = atlassianOAuthService.handleCallback(code, state);
        } catch (RuntimeException ex) {
            // Always send the user back to the frontend, even on failure —
            // otherwise they get stuck on the API domain seeing raw JSON.
            String msg = ex.getMessage() == null ? "callback_failed" : ex.getMessage();
            response.sendRedirect(frontendSuccessUrl + "?error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
            return;
        }

        // 1. Set JWT cookie so /api/auth/me works on subsequent calls
        Cookie tokenCookie = new Cookie(JwtAuthFilter.COOKIE_NAME, login.token);
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(24 * 60 * 60); // 1 day, matches JWT expiry
        tokenCookie.setHttpOnly(false);      // allow frontend JS to read
        tokenCookie.setSecure(cookieSecure);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            tokenCookie.setDomain(cookieDomain);
        }
        response.addCookie(tokenCookie);

        // 2. Build the frontend redirect: pass token in fragment + query
        String encodedToken = URLEncoder.encode(login.token, StandardCharsets.UTF_8);
        String encodedEmail = URLEncoder.encode(login.user.email, StandardCharsets.UTF_8);
        String target = frontendSuccessUrl
                + "?token=" + encodedToken
                + "&userId=" + login.user.id
                + "&email=" + encodedEmail
                + "#token=" + encodedToken
                + "&userId=" + login.user.id
                + "&email=" + encodedEmail;
        response.sendRedirect(target);
    }
}
