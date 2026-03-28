// Этот файл отвечает за точку входа приложения Spring Boot.
package ru.studentbot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.studentbot.config.BotProperties;
/*
 * Главный класс приложения.
 * Поднимает Spring Boot контекст и запускает бота.
 */
@SpringBootApplication
@EnableConfigurationProperties(BotProperties.class)
public class StudentBotApplication {
    /*
     * Точка входа в приложение.
     * Передает аргументы в SpringApplication.run и стартует сервис.
     */
    public static void main(String[] args) {
        SpringApplication.run(StudentBotApplication.class, args);
    }
}
