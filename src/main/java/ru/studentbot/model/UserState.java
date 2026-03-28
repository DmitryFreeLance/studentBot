// Этот файл отвечает за состояние пользователя в диалоге.
package ru.studentbot.model;
/*
 * Состояние пользователя в диалоге.
 * Хранит шаг сценария, данные шага и признак админа.
 */
public class UserState {
    private final long userId;
    private final String state;
    private final String data;
    private final boolean admin;
    /*
     * Конструктор состояния.
     * Заполняет все поля из записи в БД.
     */
    public UserState(long userId, String state, String data, boolean admin) {
        this.userId = userId;
        this.state = state;
        this.data = data;
        this.admin = admin;
    }
    /*
     * Возвращает id пользователя.
     * Используется для привязки состояния к человеку.
     */
    public long getUserId() {
        return userId;
    }
    /*
     * Возвращает текущий шаг сценария.
     * По нему выбирается следующая логика.
     */
    public String getState() {
        return state;
    }
    /*
     * Возвращает дополнительные данные шага.
     * Например, id мероприятия при записи.
     */
    public String getData() {
        return data;
    }
    /*
     * Возвращает признак админа.
     * Определяет доступ к админ-меню.
     */
    public boolean isAdmin() {
        return admin;
    }
}
