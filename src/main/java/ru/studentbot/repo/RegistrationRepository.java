// Этот файл отвечает за сохранение заявок на участие.
package ru.studentbot.repo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.studentbot.model.Category;
import ru.studentbot.model.RegistrationInfo;
import java.time.Instant;
import java.util.List;
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

    /*
     * Возвращает заявки по категории мероприятий.
     * Используется админ-панелью для списка записавшихся.
     */
    public List<RegistrationInfo> findByCategory(Category category) {
        return jdbcTemplate.query(
                "SELECT r.event_id, e.title, e.category, r.user_id, r.full_text, r.created_at " +
                        "FROM registrations r " +
                        "JOIN events e ON e.id = r.event_id " +
                        "WHERE e.category = ? " +
                        "ORDER BY r.created_at DESC",
                (rs, rowNum) -> new RegistrationInfo(
                        rs.getLong("event_id"),
                        rs.getString("title"),
                        Category.valueOf(rs.getString("category")),
                        rs.getLong("user_id"),
                        rs.getString("full_text"),
                        rs.getLong("created_at")
                ),
                category.name()
        );
    }
}
