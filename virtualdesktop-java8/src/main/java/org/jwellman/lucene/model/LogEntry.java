package org.jwellman.lucene.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A single line in the Lucene management activity log.
 */
public class LogEntry {

    public enum Level { INFO, SUCCESS, ERROR }

    private final Level level;
    private final String timestamp;
    private final String message;

    public LogEntry(Level level, String message) {
        this.level = level;
        this.timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        this.message = message;
    }

    public Level getLevel() {
        return level;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + level.name() + "] [" + timestamp + "] " + message;
    }
}
