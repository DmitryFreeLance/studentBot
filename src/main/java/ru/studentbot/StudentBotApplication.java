package ru.studentbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.studentbot.config.BotProperties;

@SpringBootApplication
@EnableConfigurationProperties(BotProperties.class)
public class StudentBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudentBotApplication.class, args);
    }
}
