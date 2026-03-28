// Этот файл отвечает за эндпоинт проверки живости сервиса.
package ru.studentbot.web;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
/*
 * HTTP-контроллер для проверки живости сервиса.
 * Нужен для мониторинга и контейнерных проверок.
 */
@RestController
public class HealthController {
    /*
     * Возвращает простой ответ "ok".
     * Используется внешними системами как healthcheck.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
