package org.jwellman.demo.calendar;

import java.time.LocalDate;

public class CalendarEvent {

    private final String name;
    private final LocalDate date;
    private final EventCategory category;
    private final String description;

    public CalendarEvent(String name, LocalDate date, EventCategory category, String description) {
        this.name = name;
        this.date = date;
        this.category = category;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public EventCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }
}
