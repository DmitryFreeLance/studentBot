// Этот файл отвечает за хранение состояний пользователей.
package ru.studentbot.repo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.studentbot.model.UserState;
import java.time.Instant;
import java.util.List;
/*
 * Репозиторий состояний пользователей.
 * Хранит текущий шаг диалога для каждого пользователя.
 */
@Repository
public class StateRepository {
    private final JdbcTemplate jdbcTemplate;
    /*
     * Конструктор репозитория.
     * Получает JdbcTemplate для работы с БД.
     */
    public StateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    /*
     * Загружает состояние пользователя по его id.
     * Возвращает null, если состояния нет.
     */
    public UserState findByUserId(long userId) {
        List<UserState> states = jdbcTemplate.query(
                "SELECT user_id, state, data, is_admin FROM user_state WHERE user_id = ?",
                (rs, rowNum) -> new UserState(
                        rs.getLong("user_id"),
                        rs.getString("state"),
                        rs.getString("data"),
                        rs.getInt("is_admin") == 1
                ),
                userId
        );
        return states.isEmpty() ? null : states.get(0);
    }
    /*
     * Сохраняет или обновляет состояние пользователя.
     * Использует upsert, чтобы не создавать дублей.
     */
    public void save(long userId, String state, String data, boolean isAdmin) {
        long now = Instant.now().getEpochSecond();
        jdbcTemplate.update(
                "INSERT INTO user_state(user_id, state, data, is_admin, updated_at) VALUES(?,?,?,?,?) " +
                        "ON CONFLICT(user_id) DO UPDATE SET state=excluded.state, data=excluded.data, is_admin=excluded.is_admin, updated_at=excluded.updated_at",
                userId, state, data, isAdmin ? 1 : 0, now
        );
    }
    /*
     * Сбрасывает состояние пользователя в IDLE.
     * По флагу можно сохранить признак админа.
     */
    public void clear(long userId, boolean keepAdmin) {
        long now = Instant.now().getEpochSecond();
        jdbcTemplate.update(
                "INSERT INTO user_state(user_id, state, data, is_admin, updated_at) VALUES(?,?,?,?,?) " +
                        "ON CONFLICT(user_id) DO UPDATE SET state=excluded.state, data=excluded.data, is_admin=excluded.is_admin, updated_at=excluded.updated_at",
                userId, "IDLE", null, keepAdmin ? 1 : 0, now
        );
    }
}
