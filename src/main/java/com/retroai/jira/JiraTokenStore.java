package com.retroai.jira;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-memory token store. Jira API tokens are not persisted; leader
 * re-enters them after restart, per spec.
 */
@Component
public class JiraTokenStore {

    private final Map<Long, String> tokensByConnectionId = new ConcurrentHashMap<>();

    public void put(Long connectionId, String token) {
        tokensByConnectionId.put(connectionId, token);
    }

    public String get(Long connectionId) {
        return tokensByConnectionId.get(connectionId);
    }

    public void remove(Long connectionId) {
        tokensByConnectionId.remove(connectionId);
    }
}
