package ru.studentbot.model;

public class UserState {
    private final long userId;
    private final String state;
    private final String data;
    private final boolean admin;

    public UserState(long userId, String state, String data, boolean admin) {
        this.userId = userId;
        this.state = state;
        this.data = data;
        this.admin = admin;
    }

    public long getUserId() {
        return userId;
    }

    public String getState() {
        return state;
    }

    public String getData() {
        return data;
    }

    public boolean isAdmin() {
        return admin;
    }
}
