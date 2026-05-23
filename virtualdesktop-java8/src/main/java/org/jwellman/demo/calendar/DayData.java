package org.jwellman.demo.calendar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DayData {

    private final LocalDate date;
    private final List<CalendarEvent> events;
    private final boolean inYear;

    public DayData(LocalDate date, List<CalendarEvent> events, boolean inYear) {
        this.date = date;
        this.events = new ArrayList<CalendarEvent>(events);
        this.inYear = inYear;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<CalendarEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public boolean isInYear() {
        return inYear;
    }

    public boolean hasEvents() {
        return !events.isEmpty();
    }
}
