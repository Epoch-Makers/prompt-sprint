package com.retroai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retroai.dto.AuthDtos;
import com.retroai.entity.User;
import com.retroai.exception.ApiException;
import com.retroai.jira.AtlassianTokenStore;
import com.retroai.repository.UserRepository;
import com.retroai.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Atlassian OAuth 2.0 (3LO) flow.
 *
 * 1. {@link #buildAuthorizeUrl(String)} → frontend or controller redirects user
 * 2. Atlassian redirects back to /api/auth/atlassian/callback?code=...
 * 3. {@link #handleCallback(String, String)} exchanges code → access token,
 *    fetches /me + /oauth/token/accessible-resources, find-or-create local
 *    user, stores the Atlassian session for downstream Jira API calls,
 *    returns LoginResponse with our JWT.
 */
@Service
public class AtlassianOAuthService {

    private static final Logger log = LoggerFactory.getLogger(AtlassianOAuthService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ACCESSIBLE_RESOURCES_URL = "https://api.atlassian.com/oauth/token/accessible-resources";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AtlassianTokenStore tokenStore;
    private final WebClient webClient = WebClient.builder().build();

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizeUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String scope;

    public AtlassianOAuthService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 AtlassianTokenStore tokenStore,
                                 @Value("${atlassian.oauth.client-id:}") String clientId,
                                 @Value("${atlassian.oauth.client-secret:}") String clientSecret,
                                 @Value("${atlassian.oauth.redirect-uri}") String redirectUri,
                                 @Value("${atlassian.oauth.authorize-url}") String authorizeUrl,
                                 @Value("${atlassian.oauth.token-url}") String tokenUrl,
                                 @Value("${atlassian.oauth.userinfo-url}") String userInfoUrl,
                                 @Value("${atlassian.oauth.scope}") String scope) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.clientId = trim(clientId);
        this.clientSecret = trim(clientSecret);
        this.redirectUri = trim(redirectUri);
        this.authorizeUrl = trim(authorizeUrl);
        this.tokenUrl = trim(tokenUrl);
        this.userInfoUrl = trim(userInfoUrl);
        this.scope = trim(scope);
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * Builds the Atlassian authorization URL the user should be redirected to.
     */
    public String buildAuthorizeUrl(String state) {
        if (!isConfigured()) {
            throw ApiException.serviceUnavailable(
                    "ATLASSIAN_OAUTH_NOT_CONFIGURED",
                    "Atlassian ile giriş şu anda yapılandırılmadı. Lütfen yönetici ile iletişime geçin.");
        }
        String url = UriComponentsBuilder.fromHttpUrl(authorizeUrl)
                .queryParam("audience", "api.atlassian.com")
                .queryParam("client_id", clientId)
                .queryParam("scope", scope)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("response_type", "code")
                .queryParam("prompt", "consent")
                .build(false)
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        log.info("Atlassian authorize redirect → {}", url);
        return url;
    }

    /**
     * Exchanges authorization code for access token, fetches user profile +
     * accessible Atlassian resources, find-or-create the local user, stores
     * the Atlassian session keyed by our userId, and mints our JWT.
     */
    @Transactional
    public AuthDtos.LoginResponse handleCallback(String code, String state) {
        if (!isConfigured()) {
            throw ApiException.serviceUnavailable(
                    "ATLASSIAN_OAUTH_NOT_CONFIGURED",
                    "Atlassian ile giriş şu anda yapılandırılmadı.");
        }
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("Missing authorization code");
        }

        log.info("Atlassian callback received — exchanging code for token (state={})", state);

        // 1. Exchange code for access token
        JsonNode tokenResp = exchangeCodeForToken(code);
        String accessToken = tokenResp.get("access_token").asText();
        String refreshToken = tokenResp.path("refresh_token").asText(null);
        long expiresInSec = tokenResp.path("expires_in").asLong(3600);
        Instant expiresAt = Instant.now().plusSeconds(expiresInSec);
        log.info("Atlassian token exchange OK — expiresIn={}s", expiresInSec);

        // 2. Fetch user profile
        JsonNode profile = callAtlassian(userInfoUrl, accessToken, "/me");
        if (profile == null) {
            throw ApiException.unauthorized("Atlassian profile fetch returned empty");
        }
        log.debug("Atlassian /me payload keys: {}", profile.fieldNames());

        String email = textOrNull(profile, "email");
        if (email == null) email = textOrNull(profile, "account_email");
        String name = textOrNull(profile, "name");
        if (name == null) name = textOrNull(profile, "nickname");
        String accountId = textOrNull(profile, "account_id");
        if (accountId == null) accountId = textOrNull(profile, "accountId");

        // Atlassian users with private email return email=null. Synthesise one
        // from account_id so we can still create a local user.
        if (email == null && accountId != null) {
            email = accountId + "@atlassian.local";
            log.info("Atlassian /me did not include email (private profile) — using synthetic email");
        }
        if (email == null) {
            log.error("Atlassian /me response had neither email nor account_id — payload: {}", profile);
            throw ApiException.unauthorized("Atlassian profile did not include email or account_id");
        }
        if (name == null) name = email;

        // 3. find-or-create local user
        final String userEmail = email;
        final String userName = name;
        User user = userRepository.findByEmail(userEmail).orElseGet(() -> {
            User u = new User();
            u.setEmail(userEmail);
            u.setFullName(userName);
            u.setPasswordHash(null); // OAuth-only — no local password
            u.setAuthProvider(com.retroai.enums.AuthProvider.ATLASSIAN);
            User saved = userRepository.save(u);
            log.info("Atlassian login created new local user — id={} email={}", saved.getId(), saved.getEmail());
            return saved;
        });

        // 4. Fetch accessible Atlassian sites (Jira / Confluence resources)
        String cloudId = null;
        String siteUrl = null;
        try {
            JsonNode resources = callAtlassian(ACCESSIBLE_RESOURCES_URL, accessToken, "/accessible-resources");
            if (resources != null && resources.isArray() && resources.size() > 0) {
                JsonNode first = resources.get(0);
                cloudId = textOrNull(first, "id");
                siteUrl = textOrNull(first, "url");
                log.info("Atlassian accessible resource — cloudId={} url={}", cloudId, siteUrl);
            } else {
                log.warn("Atlassian /accessible-resources returned no sites — user has no Jira/Confluence access");
            }
        } catch (Exception e) {
            // not fatal — login still succeeds, just no Jira data source
            log.warn("Failed to fetch /accessible-resources: {}", e.getMessage());
        }

        // 5. Store session for downstream Jira API calls
        tokenStore.put(user.getId(), new AtlassianTokenStore.Session(
                accessToken, refreshToken, cloudId, siteUrl, userEmail, userName, expiresAt));

        // 6. Issue our JWT
        String jwt = jwtService.generateToken(user.getId(), user.getEmail());
        log.info("Atlassian login complete — userId={} email={} cloudId={}",
                user.getId(), user.getEmail(), cloudId);
        return new AuthDtos.LoginResponse(jwt,
                new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAuthProvider()));
    }

    private JsonNode exchangeCodeForToken(String code) {
        ObjectNode tokenReq = MAPPER.createObjectNode();
        tokenReq.put("grant_type", "authorization_code");
        tokenReq.put("client_id", clientId);
        tokenReq.put("client_secret", clientSecret);
        tokenReq.put("code", code);
        tokenReq.put("redirect_uri", redirectUri);
        try {
            JsonNode resp = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(tokenReq.toString())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            if (resp == null || !resp.has("access_token")) {
                log.error("Atlassian token response missing access_token: {}", resp);
                throw ApiException.unauthorized("Atlassian did not return an access token");
            }
            return resp;
        } catch (WebClientResponseException wcre) {
            log.error("Atlassian /oauth/token returned {} — body: {}",
                    wcre.getStatusCode(), wcre.getResponseBodyAsString());
            throw ApiException.unauthorized("Atlassian token exchange failed: "
                    + wcre.getStatusCode() + " — check client_secret and redirect_uri match");
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Atlassian token exchange threw", e);
            throw ApiException.unauthorized("Atlassian token exchange failed: " + e.getMessage());
        }
    }

    private JsonNode callAtlassian(String url, String accessToken, String label) {
        try {
            return webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
        } catch (WebClientResponseException wcre) {
            log.error("Atlassian {} returned {} — body: {}",
                    label, wcre.getStatusCode(), wcre.getResponseBodyAsString());
            throw ApiException.unauthorized("Atlassian " + label + " call failed: "
                    + wcre.getStatusCode());
        } catch (Exception e) {
            log.error("Atlassian {} threw", label, e);
            throw ApiException.unauthorized("Atlassian " + label + " call failed: " + e.getMessage());
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }
}
