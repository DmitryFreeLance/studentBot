// Этот файл отвечает за типы мероприятий и их отображение.
package ru.studentbot.model;
import java.util.Locale;
/*
 * Перечисление категорий школьных мероприятий.
 * Хранит отображаемое название и эмодзи для интерфейса.
 */
public enum Category {
    CULTURAL("Культурные", "🎭"),
    ENTERTAINMENT("Развлекательные", "🎉"),
    SPORTS("Спортивные", "🏆");
    private final String displayName;
    private final String emoji;
    /*
     * Конструктор категории.
     * Сохраняет читаемое название и эмодзи.
     */
    Category(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }
    /*
     * Возвращает название категории для пользователя.
     * Используется в кнопках и сообщениях.
     */
    public String getDisplayName() {
        return displayName;
    }
    /*
     * Возвращает эмодзи категории.
     * Добавляется к сообщениям для визуального акцента.
     */
    public String getEmoji() {
        return emoji;
    }
    /*
     * Преобразует строку payload в категорию.
     * Нужен для обработки нажатий inline-кнопок.
     */
    public static Category fromPayload(String payload) {
        if (payload == null) {
            return null;
        }
        String value = payload.trim().toUpperCase(Locale.ROOT);
        for (Category category : values()) {
            if (category.name().equals(value)) {
                return category;
            }
        }
        return null;
    }
}
