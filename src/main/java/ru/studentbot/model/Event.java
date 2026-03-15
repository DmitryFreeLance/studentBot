package ru.studentbot.model;

public class Event {
    private final long id;
    private final Category category;
    private final String title;
    private final String details;

    public Event(long id, Category category, String title, String details) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.details = details;
    }

    public long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }
}
