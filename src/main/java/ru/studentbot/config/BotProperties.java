package ru.studentbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bot")
public class BotProperties {
    private String token;
    private String apiBase;
    private LongPolling longPolling = new LongPolling();

    public static class LongPolling {
        private boolean enabled = true;
        private int timeout = 30;
        private int limit = 100;
        private String types = "message_created,message_callback";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public String getTypes() {
            return types;
        }

        public void setTypes(String types) {
            this.types = types;
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApiBase() {
        return apiBase;
    }

    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }

    public LongPolling getLongPolling() {
        return longPolling;
    }

    public void setLongPolling(LongPolling longPolling) {
        this.longPolling = longPolling;
    }
}
