package org.jwellman.virtualdesktop.security;

/**
 *
 * @author Rick
 */
public class ExitException extends SecurityException {

    public final int status;

    public ExitException(int status) {
        super("There is no escape!");
        this.status = status;
    }

}