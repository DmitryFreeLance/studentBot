// Этот файл отвечает за сохранение заявок на участие.
package ru.studentbot.repo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Instant;
/*
 * Репозиторий заявок на участие.
 * Сохраняет сообщения пользователей в таблицу registrations.
 */
@Repository
public class RegistrationRepository {
    private final JdbcTemplate jdbcTemplate;
    /*
     * Конструктор репозитория.
     * Получает JdbcTemplate для выполнения SQL.
     */
    public RegistrationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    /*
     * Сохраняет заявку пользователя.
     * Записывает id мероприятия, id пользователя и текст заявки.
     */
    public void insert(long eventId, long userId, String fullText) {
        long now = Instant.now().getEpochSecond();
        jdbcTemplate.update(
                "INSERT INTO registrations(event_id, user_id, full_text, created_at) VALUES(?,?,?,?)",
                eventId, userId, fullText, now
        );
    }
}
