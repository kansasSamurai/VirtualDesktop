package org.jwellman.virtualdesktop.docking;

/**
 * Exception thrown when docking operations fail.
 *
 * @author Rick Wellman
 */
public class DockingException extends Exception {

    private static final long serialVersionUID = 1L;

    public DockingException(String message) {
        super(message);
    }

    public DockingException(String message, Throwable cause) {
        super(message, cause);
    }
}
