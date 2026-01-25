package org.jwellman.scripttester;

/**
 * Status constants for script line execution state.
 *
 * @author Rick Wellman
 */
public final class Status {

    public static final String PENDING = "pending";
    public static final String CURRENT = "current";
    public static final String COMPLETED = "completed";
    public static final String ERROR = "error";
    public static final String SKIPPED = "skipped";

    private Status() {
        // Prevent instantiation
    }
}
