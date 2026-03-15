package ru.studentbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.studentbot.config.BotProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class MaxApiClient {
    private static final Logger log = LoggerFactory.getLogger(MaxApiClient.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final BotProperties properties;

    public MaxApiClient(ObjectMapper objectMapper, BotProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void sendMessageToUser(long userId, ObjectNode message) {
        String path = "/messages?user_id=" + userId;
        sendJson(path, message);
    }

    public void sendMessageToChat(long chatId, ObjectNode message) {
        String path = "/messages?chat_id=" + chatId;
        sendJson(path, message);
    }

    public void answerCallback(String callbackId, ObjectNode message) {
        String encoded = URLEncoder.encode(callbackId, StandardCharsets.UTF_8);
        String path = "/answers?callback_id=" + encoded;
        ObjectNode body = objectMapper.createObjectNode();
        body.set("message", message);
        sendJson(path, body);
    }

    public JsonNode getUpdates(Long marker, int timeoutSeconds, int limit, String types) {
        StringBuilder path = new StringBuilder("/updates?timeout=")
                .append(timeoutSeconds)
                .append("&limit=")
                .append(limit);
        if (types != null && !types.isBlank()) {
            path.append("&types=").append(URLEncoder.encode(types, StandardCharsets.UTF_8));
        }
        if (marker != null) {
            path.append("&marker=").append(marker);
        }
        return getJson(path.toString());
    }

    private void sendJson(String path, ObjectNode body) {
        String token = properties.getToken();
        if (token == null || token.isBlank()) {
            log.warn("MAX token is empty. Skip sending request to {}", path);
            return;
        }
        try {
            String apiBase = properties.getApiBase();
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + path))
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("MAX API request failed. status={}, body={}", response.statusCode(), response.body());
            }
        } catch (IOException e) {
            log.warn("MAX API request error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("MAX API request interrupted", e);
        }
    }

    private JsonNode getJson(String path) {
        String token = properties.getToken();
        if (token == null || token.isBlank()) {
            log.warn("MAX token is empty. Skip sending request to {}", path);
            return null;
        }
        try {
            String apiBase = properties.getApiBase();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + path))
                    .header("Authorization", token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("MAX API request failed. status={}, body={}", response.statusCode(), response.body());
                return null;
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            log.warn("MAX API request error", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("MAX API request interrupted", e);
            return null;
        }
    }
}
