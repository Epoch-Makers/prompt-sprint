package com.retroai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Hackathon-mode current-user accessor.
 *
 * Authentication is no longer enforced, so when nothing populates the
 * {@code SecurityContext} we fall back to a default user (id=1, the
 * seeded "Ayşe Yılmaz"). This keeps controllers that rely on
 * {@link #id()} simple — they never have to handle null.
 */
public final class CurrentUser {

    /** Seeded leader of the demo team — see {@code data.sql}. */
    public static final Long DEFAULT_USER_ID = 1L;
    public static final String DEFAULT_USER_EMAIL = "ayse@example.com";

    private CurrentUser() {}

    public static AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthenticatedUser) {
            return (AuthenticatedUser) auth.getPrincipal();
        }
        return new AuthenticatedUser(DEFAULT_USER_ID, DEFAULT_USER_EMAIL);
    }

    public static Long id() {
        return get().getId();
    }
}
