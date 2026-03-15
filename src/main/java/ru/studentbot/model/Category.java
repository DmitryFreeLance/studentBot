package ru.studentbot.model;

import java.util.Locale;

public enum Category {
    CULTURAL("Культурные", "🎭"),
    ENTERTAINMENT("Развлекательные", "🎉"),
    SPORTS("Спортивные", "🏆");

    private final String displayName;
    private final String emoji;

    Category(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

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
