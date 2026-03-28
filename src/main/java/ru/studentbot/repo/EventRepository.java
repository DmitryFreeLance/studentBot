// Этот файл отвечает за доступ к мероприятиям в базе данных.
package ru.studentbot.repo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.studentbot.model.Category;
import ru.studentbot.model.Event;
import java.time.Instant;
import java.util.List;
/*
 * Репозиторий мероприятий.
 * Выполняет SQL-запросы к таблице events.
 */
@Repository
public class EventRepository {
    private final JdbcTemplate jdbcTemplate;
    /*
     * Конструктор репозитория.
     * Получает JdbcTemplate для работы с SQLite.
     */
    public EventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    /*
     * Возвращает активные мероприятия по категории.
     * Сортирует по убыванию id.
     */
    public List<Event> findActiveByCategory(Category category) {
        return jdbcTemplate.query(
                "SELECT id, category, title, details FROM events WHERE is_active = 1 AND category = ? ORDER BY id DESC",
                (rs, rowNum) -> new Event(
                        rs.getLong("id"),
                        Category.valueOf(rs.getString("category")),
                        rs.getString("title"),
                        rs.getString("details")
                ),
                category.name()
        );
    }
    /*
     * Возвращает все активные мероприятия.
     * Используется для общего админ-списка.
     */
    public List<Event> findAllActive() {
        return jdbcTemplate.query(
                "SELECT id, category, title, details FROM events WHERE is_active = 1 ORDER BY id DESC",
                (rs, rowNum) -> new Event(
                        rs.getLong("id"),
                        Category.valueOf(rs.getString("category")),
                        rs.getString("title"),
                        rs.getString("details")
                )
        );
    }
    /*
     * Ищет мероприятие по id.
     * Возвращает null, если запись не найдена.
     */
    public Event findById(long id) {
        List<Event> events = jdbcTemplate.query(
                "SELECT id, category, title, details FROM events WHERE id = ?",
                (rs, rowNum) -> new Event(
                        rs.getLong("id"),
                        Category.valueOf(rs.getString("category")),
                        rs.getString("title"),
                        rs.getString("details")
                ),
                id
        );
        return events.isEmpty() ? null : events.get(0);
    }
    /*
     * Добавляет новое мероприятие в базу.
     * Возвращает id созданной записи.
     */
    public long insert(Category category, String title, String details) {
        long now = Instant.now().getEpochSecond();
        jdbcTemplate.update(
                "INSERT INTO events(category, title, details, is_active, updated_at) VALUES(?,?,?,?,?)",
                category.name(), title, details, 1, now
        );
        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        return id == null ? -1 : id;
    }
    /*
     * Обновляет название и описание мероприятия.
     * Используется в админ-редактировании.
     */
    public void update(long id, String title, String details) {
        long now = Instant.now().getEpochSecond();
        jdbcTemplate.update(
                "UPDATE events SET title = ?, details = ?, updated_at = ? WHERE id = ?",
                title, details, now, id
        );
    }
    /*
     * Помечает мероприятие неактивным.
     * Фактически это логическое удаление.
     */
    public void deactivate(long id) {
        long now = Instant.now().getEpochSecond();
        jdbcTemplate.update(
                "UPDATE events SET is_active = 0, updated_at = ? WHERE id = ?",
                now, id
        );
    }
    /*
     * Возвращает количество всех мероприятий.
     * Нужен для проверки пустой базы.
     */
    public int countAll() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
        return count == null ? 0 : count;
    }
}
