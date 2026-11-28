package org.katacode.agents.auditor.model;

import java.util.Locale;

/**
 * Posture of a registered project, as shown in the project list.
 */
public enum ProjectStatus {

    OK,
    WARN,
    DRIFT,
    WAIVED;

    /**
     * Lenient parse: a missing or unrecognised value becomes {@link #OK}, so a
     * typo in the configuration file cannot stop the application from starting.
     */
    public static ProjectStatus parse(String value) {
        if (value != null) {
            String name = value.trim().toUpperCase(Locale.ROOT);
            for (ProjectStatus status : values()) {
                if (status.name().equals(name)) {
                    return status;
                }
            }
        }

        return OK;
    }

    public boolean needsAttention() {
        return this == WARN || this == DRIFT;
    }
}
