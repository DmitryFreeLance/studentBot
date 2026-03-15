package ru.studentbot.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class RegistrationRepository {
    private final JdbcTemplate jdbcTemplate;

    public RegistrationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(long eventId, long userId, String fullText) {
        long now = Instant.now().getEpochSecond();
        jdbcTemplate.update(
                "INSERT INTO registrations(event_id, user_id, full_text, created_at) VALUES(?,?,?,?)",
                eventId, userId, fullText, now
        );
    }
}
