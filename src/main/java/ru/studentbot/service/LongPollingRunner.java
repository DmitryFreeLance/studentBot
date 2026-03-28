// Этот файл отвечает за получение обновлений через long polling.
package ru.studentbot.service;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import ru.studentbot.config.BotProperties;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
/*
 * Фоновый цикл получения обновлений через long polling.
 * Запускается после старта приложения и работает постоянно.
 */
@Component
public class LongPollingRunner {
    private static final Logger log = LoggerFactory.getLogger(LongPollingRunner.class);
    private final MaxApiClient apiClient;
    private final BotService botService;
    private final BotProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "max-long-poll");
        t.setDaemon(true);
        return t;
    });
    private volatile Long marker;
    /*
     * Конструктор фонового раннера.
     * Сохраняет зависимости для опроса и обработки апдейтов.
     */
    public LongPollingRunner(MaxApiClient apiClient, BotService botService, BotProperties properties) {
        this.apiClient = apiClient;
        this.botService = botService;
        this.properties = properties;
    }
    /*
     * Запускает фоновый поток после старта приложения.
     * Не стартует цикл, если long polling отключен.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.getLongPolling().isEnabled()) {
            log.info("Long polling disabled by config.");
            return;
        }
        if (running.compareAndSet(false, true)) {
            executor.submit(this::pollLoop);
        }
    }
    /*
     * Останавливает фоновый поток при завершении приложения.
     * Аккуратно завершает executor и поток.
     */
    @PreDestroy
    public void stop() {
        running.set(false);
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    /*
     * Основной цикл опроса.
     * Запрашивает обновления, обрабатывает их и двигает marker.
     */
    private void pollLoop() {
        BotProperties.LongPolling cfg = properties.getLongPolling();
        long backoffMillis = 1000;
        while (running.get()) {
            try {
                JsonNode response = apiClient.getUpdates(marker, cfg.getTimeout(), cfg.getLimit(), cfg.getTypes());
                if (response == null) {
                    sleepQuietly(backoffMillis);
                    backoffMillis = nextBackoff(backoffMillis);
                    continue;
                }
                backoffMillis = 1000;
                JsonNode updates = response.path("updates");
                if (updates.isArray()) {
                    for (JsonNode update : updates) {
                        try {
                            botService.handleUpdateNode(update);
                        } catch (Exception e) {
                            log.warn("Update handling error. Skip update.", e);
                        }
                    }
                }
                if (response.hasNonNull("marker")) {
                    marker = response.get("marker").asLong();
                }
            } catch (Exception e) {
                log.warn("Long polling error", e);
                sleepQuietly(backoffMillis);
                backoffMillis = nextBackoff(backoffMillis);
            }
        }
    }
    /*
     * Делает паузу между запросами при ошибках.
     * Корректно обрабатывает прерывание потока.
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long nextBackoff(long currentMillis) {
        long next = currentMillis * 2;
        return Math.min(next, 30000);
    }
}
