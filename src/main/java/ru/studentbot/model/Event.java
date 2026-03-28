// Этот файл отвечает за модель школьного мероприятия.
package ru.studentbot.model;
/*
 * Модель мероприятия.
 * Содержит id, категорию, название и подробное описание.
 */
public class Event {
    private final long id;
    private final Category category;
    private final String title;
    private final String details;
    /*
     * Конструктор мероприятия.
     * Заполняет все поля модели из БД.
     */
    public Event(long id, Category category, String title, String details) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.details = details;
    }
    /*
     * Возвращает идентификатор мероприятия.
     * Нужен для ссылок и действий в админке.
     */
    public long getId() {
        return id;
    }
    /*
     * Возвращает категорию мероприятия.
     * Используется для группировки и фильтрации.
     */
    public Category getCategory() {
        return category;
    }
    /*
     * Возвращает название мероприятия.
     * Показывается в списках и кнопках.
     */
    public String getTitle() {
        return title;
    }
    /*
     * Возвращает подробное описание мероприятия.
     * Отправляется пользователю целиком.
     */
    public String getDetails() {
        return details;
    }
}
