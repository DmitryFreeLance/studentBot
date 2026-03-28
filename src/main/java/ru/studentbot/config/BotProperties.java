// Этот файл отвечает за чтение и хранение настроек бота.
package ru.studentbot.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
/*
 * Модель конфигурации бота.
 * Значения приходят из application.yml и переменных окружения.
 */
@ConfigurationProperties(prefix = "bot")
public class BotProperties {
    private String token;
    private String apiBase;
    private LongPolling longPolling = new LongPolling();
    /*
     * Настройки long polling.
     * Управляет включением, таймаутом, лимитом и типами обновлений.
     * (используется меод взаимодействия long polling, а не webhook!)
     */
    public static class LongPolling {
        private boolean enabled = true;
        private int timeout = 30;
        private int limit = 100;
        private String types = "message_created,message_callback";
        /*
         * Возвращает флаг включения long polling.
         * По нему решаем, запускать ли фоновый цикл.
         */
        public boolean isEnabled() {
            return enabled;
        }
        /*
         * Устанавливает флаг включения long polling.
         * Позволяет включать или отключать получение обновлений.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        /*
         * Возвращает таймаут ожидания апдейтов в секундах.
         * Это время сервер держит запрос открытым.
         */
        public int getTimeout() {
            return timeout;
        }
        /*
         * Устанавливает таймаут ожидания апдейтов.
         * Нужен для настройки скорости реакции и нагрузки.
         */
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
        /*
         * Возвращает максимальное число апдейтов за один запрос.
         * Помогает контролировать размер ответа.
         */
        public int getLimit() {
            return limit;
        }
        /*
         * Устанавливает лимит апдейтов за запрос.
         * Позволяет настроить объем обработки за один цикл.
         */
        public void setLimit(int limit) {
            this.limit = limit;
        }
        /*
         * Возвращает фильтр типов апдейтов.
         * Например, только сообщения и callback-и.
         */
        public String getTypes() {
            return types;
        }
        /*
         * Устанавливает фильтр типов апдейтов.
         * Нужен, чтобы получать только нужные события.
         */
        public void setTypes(String types) {
            this.types = types;
        }
    }
    /*
     * Возвращает токен бота.
     * Используется для авторизации запросов к API.
     */
    public String getToken() {
        return token;
    }
    /*
     * Устанавливает токен бота.
     * Дает доступ к API MAX.
     */
    public void setToken(String token) {
        this.token = token;
    }
    /*
     * Возвращает базовый URL API.
     * Позволяет работать с продом или другим окружением.
     */
    public String getApiBase() {
        return apiBase;
    }
    /*
     * Устанавливает базовый URL API.
     * Нужен для перенастройки адреса сервиса.
     */
    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }
    /*
     * Возвращает объект настроек long polling.
     * Используется циклом получения обновлений.
     */
    public LongPolling getLongPolling() {
        return longPolling;
    }
    /*
     * Устанавливает объект настроек long polling.
     * Позволяет подменить настройки целиком.
     */
    public void setLongPolling(LongPolling longPolling) {
        this.longPolling = longPolling;
    }
}
