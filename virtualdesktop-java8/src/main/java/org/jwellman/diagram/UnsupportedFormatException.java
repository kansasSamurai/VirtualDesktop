package org.jwellman.diagram;

/**
 * Thrown when a .dgx file's format version is too new for this build to read.
 */
public class UnsupportedFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnsupportedFormatException(String message) {
        super(message);
    }
}
