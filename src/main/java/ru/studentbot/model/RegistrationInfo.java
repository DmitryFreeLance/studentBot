// Этот файл отвечает за модель заявки для админ-вывода.
package ru.studentbot.model;
/*
 * Модель информации о заявке.
 * Используется при выводе списка записавшихся.
 */
public record RegistrationInfo(
        long eventId,
        String eventTitle,
        Category category,
        long userId,
        String fullText,
        long createdAt
) {
}
