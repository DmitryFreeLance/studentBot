// Этот файл отвечает за HTTP-клиент для API MAX.
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
import java.time.Duration;
/*
 * HTTP-клиент для API MAX.
 * Отправляет сообщения, ответы на callback и получает апдейты.
 */
@Component
public class MaxApiClient {
    private static final Logger log = LoggerFactory.getLogger(MaxApiClient.class);
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 20;
    private static final int LONG_POLLING_GRACE_SECONDS = 5;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper;
    private final BotProperties properties;
    /*
     * Конструктор клиента.
     * Принимает ObjectMapper и настройки для авторизации.
     */
    public MaxApiClient(ObjectMapper objectMapper, BotProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }
    /*
     * Отправляет сообщение пользователю по его user_id.
     * Использует POST /messages с параметром user_id.
     */
    public void sendMessageToUser(long userId, ObjectNode message) {
        String path = "/messages?user_id=" + userId;
        sendJson(path, message);
    }
    /*
     * Отправляет сообщение в чат по chat_id.
     * Использует POST /messages с параметром chat_id.
     */
    public void sendMessageToChat(long chatId, ObjectNode message) {
        String path = "/messages?chat_id=" + chatId;
        sendJson(path, message);
    }
    /*
     * Отвечает на нажатие inline-кнопки.
     * Использует POST /answers с callback_id.
     */
    public void answerCallback(String callbackId, ObjectNode message) {
        String encoded = URLEncoder.encode(callbackId, StandardCharsets.UTF_8);
        String path = "/answers?callback_id=" + encoded;
        ObjectNode body = objectMapper.createObjectNode();
        body.set("message", message);
        sendJson(path, body);
    }
    /*
     * Запрашивает обновления long polling.
     * Передает marker, timeout, limit и фильтр типов.
     */
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
        return getJson(path.toString(), timeoutSeconds);
    }
    /*
     * Внутренний метод для POST-запросов.
     * Добавляет заголовок Authorization и логирует ошибки.
     */
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
                    .timeout(Duration.ofSeconds(DEFAULT_REQUEST_TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("MAX API request failed. path={}, status={}, body={}", path, response.statusCode(), response.body());
            }
        } catch (IOException e) {
            log.warn("MAX API request error. path={}", path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("MAX API request interrupted. path={}", path, e);
        }
    }
    /*
     * Внутренний метод для GET-запросов.
     * Парсит JSON-ответ и возвращает его как JsonNode.
     */
    private JsonNode getJson(String path, Integer timeoutSeconds) {
        String token = properties.getToken();
        if (token == null || token.isBlank()) {
            log.warn("MAX token is empty. Skip sending request to {}", path);
            return null;
        }
        try {
            String apiBase = properties.getApiBase();
            Duration requestTimeout = resolveTimeout(timeoutSeconds);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + path))
                    .header("Authorization", token)
                    .timeout(requestTimeout)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("MAX API request failed. path={}, status={}, body={}", path, response.statusCode(), response.body());
                return null;
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            log.warn("MAX API request error. path={}", path, e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("MAX API request interrupted. path={}", path, e);
            return null;
        }
    }

    private Duration resolveTimeout(Integer timeoutSeconds) {
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return Duration.ofSeconds(DEFAULT_REQUEST_TIMEOUT_SECONDS);
        }
        long seconds = Math.max(timeoutSeconds + LONG_POLLING_GRACE_SECONDS, DEFAULT_REQUEST_TIMEOUT_SECONDS);
        return Duration.ofSeconds(seconds);
    }
}
