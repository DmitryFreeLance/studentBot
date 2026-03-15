package ru.studentbot.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.studentbot.model.Category;
import ru.studentbot.repo.EventRepository;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {
    private final JdbcTemplate jdbcTemplate;
    private final EventRepository eventRepository;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, EventRepository eventRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventRepository = eventRepository;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "details TEXT NOT NULL," +
                "is_active INTEGER NOT NULL DEFAULT 1," +
                "updated_at INTEGER NOT NULL" +
                ")");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS registrations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "event_id INTEGER NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "full_text TEXT NOT NULL," +
                "created_at INTEGER NOT NULL" +
                ")");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_state (" +
                "user_id INTEGER PRIMARY KEY," +
                "state TEXT NOT NULL," +
                "data TEXT," +
                "is_admin INTEGER NOT NULL DEFAULT 0," +
                "updated_at INTEGER NOT NULL" +
                ")");

        if (eventRepository.countAll() == 0) {
            seedDefaults();
        }
    }

    private void seedDefaults() {
        String cultural = "🎭 Вечер школьного театра «Живые страницы»\n\n" +
                "🗓 Дата: 22 апреля\n" +
                "🕒 Время: 16:30\n" +
                "📍 Место: актовый зал\n\n" +
                "В программе — мини-спектакли, сценические этюды и чтение отрывков " +
                "из любимых книг. Подойдёт тем, кто любит литературу, сцену и дружескую атмосферу.\n\n" +
                "Возьми хорошее настроение и пригласи друзей.";

        String entertainment = "🎉 Школьный квиз «Умники и Умницы»\n\n" +
                "🗓 Дата: 19 апреля\n" +
                "🕒 Время: 15:10\n" +
                "📍 Место: кабинет информатики\n\n" +
                "Командная игра на логику, внимание и общую эрудицию. " +
                "Будут вопросы про кино, музыку, науку и школьные лайфхаки.\n\n" +
                "Собирайте команду 4–6 человек и приходите за яркими эмоциями.";

        String sports = "🏆 Турнир по волейболу «Кубок школы»\n\n" +
                "🗓 Дата: 27 апреля\n" +
                "🕒 Время: 15:40\n" +
                "📍 Место: спортивный зал\n\n" +
                "Игры в формате коротких сетов, поддержка болельщиков и дружеский дух. " +
                "Подойдёт всем, кто любит активность и командную игру.\n\n" +
                "Форма — спортивная, обувь — сменная.";

        eventRepository.insert(Category.CULTURAL, "Вечер школьного театра «Живые страницы»", cultural);
        eventRepository.insert(Category.ENTERTAINMENT, "Квиз «Умники и Умницы»", entertainment);
        eventRepository.insert(Category.SPORTS, "Турнир по волейболу «Кубок школы»", sports);
    }
}
