package org.jwellman.demo.calendar;

import java.awt.Color;

public enum EventCategory {

    RELEASE(new Color(0x42, 0x85, 0xF4), "Release"),
    HOTFIX(new Color(0xEA, 0x43, 0x35), "Hotfix"),
    MEETING(new Color(0x34, 0xA8, 0x53), "Meeting"),
    DEADLINE(new Color(0xFF, 0x99, 0x00), "Deadline");

    private final Color color;
    private final String displayName;

    EventCategory(Color color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public Color getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }
}
