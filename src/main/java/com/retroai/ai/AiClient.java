package com.retroai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.retroai.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Thin wrapper around an LLM REST API (Anthropic or OpenAI).
 *
 * If `ai.api.key` is empty, all methods return null — service layer
 * falls back to deterministic stub output. This keeps the system
 * usable in demos without external creds.
 */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String provider;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final long timeoutMs;
    private final WebClient webClient;

    public AiClient(@Value("${ai.provider:anthropic}") String provider,
                    @Value("${ai.api.key:}") String apiKey,
                    @Value("${ai.model:claude-haiku-4-5-20251001}") String model,
                    @Value("${ai.base-url:https://api.anthropic.com/v1/messages}") String baseUrl,
                    @Value("${ai.timeout-ms:30000}") long timeoutMs) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.webClient = WebClient.builder().build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Send a prompt expecting JSON response. The provider system prompt asks for strict JSON.
     * Returns parsed JsonNode or null if not configured / on timeout / error.
     */
    public JsonNode completeJson(String systemPrompt, String userPrompt) {
        if (!isConfigured()) return null;
        try {
            if ("anthropic".equalsIgnoreCase(provider)) {
                return callAnthropic(systemPrompt, userPrompt);
            } else {
                return callOpenAi(systemPrompt, userPrompt);
            }
        } catch (java.util.concurrent.TimeoutException te) {
            throw ApiException.timeout("AI request timed out");
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.warn("AI call failed: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode callAnthropic(String systemPrompt, String userPrompt) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("system", systemPrompt + "\nRespond with strict JSON only.");
        ArrayNode messages = body.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", userPrompt);

        String response = webClient.post()
                .uri(baseUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(e -> Mono.empty())
                .block();

        if (response == null) return null;
        JsonNode root = MAPPER.readTree(response);
        JsonNode content = root.path("content");
        if (content.isArray() && content.size() > 0) {
            String text = content.get(0).path("text").asText("");
            return parseFirstJson(text);
        }
        return null;
    }

    private JsonNode callOpenAi(String systemPrompt, String userPrompt) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt + "\nRespond with strict JSON only.");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        String response = webClient.post()
                .uri(baseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(e -> Mono.empty())
                .block();

        if (response == null) return null;
        JsonNode root = MAPPER.readTree(response);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            String text = choices.get(0).path("message").path("content").asText("");
            return parseFirstJson(text);
        }
        return null;
    }

    private JsonNode parseFirstJson(String text) {
        if (text == null || text.isBlank()) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) return null;
        try {
            return MAPPER.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }
}
