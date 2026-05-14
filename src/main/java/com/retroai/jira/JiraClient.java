package com.retroai.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.retroai.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Minimal Atlassian REST API wrapper. Uses HTTP Basic with
 * `email:apiToken` base64-encoded as required by Atlassian Cloud.
 *
 * Returns null on any failure so callers can fall back to mock mode.
 */
@Component
public class JiraClient {

    private static final Logger log = LoggerFactory.getLogger(JiraClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient = WebClient.builder().build();

    public boolean verifyCredentials(String domain, String email, String apiToken) {
        String url = "https://" + domain + "/rest/api/3/myself";
        try {
            JsonNode resp = get(url, email, apiToken);
            return resp != null && resp.has("accountId");
        } catch (Exception e) {
            return false;
        }
    }

    public JsonNode get(String url, String email, String apiToken) {
        try {
            return webClient.get()
                    .uri(url)
                    .header("Authorization", basicAuth(email, apiToken))
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(e -> {
                        log.debug("Jira GET failed: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            log.debug("Jira GET error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create a Jira issue. Returns issue key on success, null on failure.
     */
    public String createIssue(String domain, String email, String apiToken,
                              String projectKey, String summary, String description,
                              String assigneeAccountId, java.util.List<String> labels) {
        ObjectNode body = MAPPER.createObjectNode();
        ObjectNode fields = body.putObject("fields");
        ObjectNode project = fields.putObject("project");
        project.put("key", projectKey);
        fields.put("summary", summary);
        ObjectNode descObj = fields.putObject("description");
        descObj.put("type", "doc");
        descObj.put("version", 1);
        ArrayNode content = descObj.putArray("content");
        ObjectNode p = content.addObject();
        p.put("type", "paragraph");
        ArrayNode pc = p.putArray("content");
        ObjectNode text = pc.addObject();
        text.put("type", "text");
        text.put("text", description == null ? "" : description);
        ObjectNode issueType = fields.putObject("issuetype");
        issueType.put("name", "Task");
        if (assigneeAccountId != null) {
            ObjectNode assignee = fields.putObject("assignee");
            assignee.put("accountId", assigneeAccountId);
        }
        if (labels != null && !labels.isEmpty()) {
            ArrayNode arr = fields.putArray("labels");
            for (String l : labels) arr.add(l);
        }

        String url = "https://" + domain + "/rest/api/3/issue";
        try {
            JsonNode resp = webClient.post()
                    .uri(url)
                    .header("Authorization", basicAuth(email, apiToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(e -> {
                        log.debug("Jira POST failed: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();
            if (resp != null && resp.has("key")) return resp.get("key").asText();
            return null;
        } catch (Exception e) {
            log.debug("Jira POST error: {}", e.getMessage());
            return null;
        }
    }

    private String basicAuth(String email, String apiToken) {
        if (email == null || apiToken == null) {
            throw ApiException.unauthorized("Jira credentials missing");
        }
        String raw = email + ":" + apiToken;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
