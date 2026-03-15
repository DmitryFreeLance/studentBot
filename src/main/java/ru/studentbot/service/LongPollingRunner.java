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

    public LongPollingRunner(MaxApiClient apiClient, BotService botService, BotProperties properties) {
        this.apiClient = apiClient;
        this.botService = botService;
        this.properties = properties;
    }

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

    private void pollLoop() {
        BotProperties.LongPolling cfg = properties.getLongPolling();
        while (running.get()) {
            try {
                JsonNode response = apiClient.getUpdates(marker, cfg.getTimeout(), cfg.getLimit(), cfg.getTypes());
                if (response == null) {
                    sleepQuietly(1000);
                    continue;
                }

                JsonNode updates = response.path("updates");
                if (updates.isArray()) {
                    for (JsonNode update : updates) {
                        botService.handleUpdateNode(update);
                    }
                }

                if (response.hasNonNull("marker")) {
                    marker = response.get("marker").asLong();
                }
            } catch (Exception e) {
                log.warn("Long polling error", e);
                sleepQuietly(1000);
            }
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
