package com.retroai.jira;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-memory store of Atlassian OAuth sessions keyed by our internal
 * userId. Holds the access token, the selected Atlassian site (cloudId +
 * URL) and the expiry, so downstream Jira REST calls can be made on the
 * user's behalf without re-asking for credentials.
 *
 * Tokens are NOT persisted: after a process restart, the user has to
 * sign in via Atlassian again.
 */
@Component
public class AtlassianTokenStore {

    public static final class Session {
        public final String accessToken;
        public final String refreshToken;
        public final String cloudId;
        public final String siteUrl;
        public final String email;
        public final String displayName;
        public final Instant expiresAt;

        public Session(String accessToken, String refreshToken, String cloudId,
                       String siteUrl, String email, String displayName, Instant expiresAt) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.cloudId = cloudId;
            this.siteUrl = siteUrl;
            this.email = email;
            this.displayName = displayName;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public void put(Long userId, Session s) {
        sessions.put(userId, s);
    }

    public Optional<Session> find(Long userId) {
        return Optional.ofNullable(sessions.get(userId));
    }

    public void remove(Long userId) {
        sessions.remove(userId);
    }
}
