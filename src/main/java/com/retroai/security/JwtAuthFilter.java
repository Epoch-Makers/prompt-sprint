package com.retroai.security;

import com.retroai.entity.User;
import com.retroai.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Opportunistic auth filter.
 *
 * Priority order:
 *   1. {@code Authorization: Bearer <jwt>}
 *   2. {@code retroai_token} cookie (JWT)
 *   3. {@code X-Guest-Session: <uuid>} header (guest session)
 *   4. {@code ?guestSessionId=<uuid>} query parameter (guest session)
 *
 * Per spec section 11, Bearer takes precedence over guest session when both
 * are sent. Never rejects — endpoints remain {@code permitAll}; if no token is
 * present, {@link CurrentUser} falls back to the default seed user.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "retroai_token";
    public static final String GUEST_HEADER = "X-Guest-Session";
    public static final String GUEST_QUERY = "guestSessionId";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // 1+2: try JWT first
        String jwt = extractJwt(req);
        boolean authenticated = false;
        if (jwt != null) {
            try {
                Claims claims = jwtService.parse(jwt);
                Long userId = Long.parseLong(claims.getSubject());
                String email = claims.get("email", String.class);
                AuthenticatedUser principal = new AuthenticatedUser(userId, email);
                setSecurityContext(principal, req, "ROLE_USER");
                authenticated = true;
            } catch (Exception ignored) {
                // invalid / expired
            }
        }

        // 3+4: fall back to guest session
        if (!authenticated) {
            String guestSessionId = extractGuestSessionId(req);
            if (guestSessionId != null) {
                Optional<User> guest = userRepository.findByGuestSessionId(guestSessionId);
                if (guest.isPresent()) {
                    User u = guest.get();
                    AuthenticatedUser principal = new AuthenticatedUser(
                            u.getId(), u.getEmail(), true, u.getGuestRetroId(), null);
                    setSecurityContext(principal, req, "ROLE_GUEST");
                }
            }
        }

        chain.doFilter(req, res);
    }

    private void setSecurityContext(AuthenticatedUser principal, HttpServletRequest req, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(role)));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String extractJwt(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private String extractGuestSessionId(HttpServletRequest req) {
        String headerVal = req.getHeader(GUEST_HEADER);
        if (headerVal != null && !headerVal.isBlank()) return headerVal.trim();
        String queryVal = req.getParameter(GUEST_QUERY);
        if (queryVal != null && !queryVal.isBlank()) return queryVal.trim();
        return null;
    }
}
