package org.jwellman.console;

/**
 * Exception thrown by interpreter adapters when evaluation fails.
 *
 * <p>This provides a common exception type that wraps interpreter-specific
 * exceptions (BeanShell's EvalError, Nashorn's ScriptException, etc.).</p>
 *
 * @author Rick Wellman
 */
public class InterpreterException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * The line number where the error occurred, or -1 if unknown.
     */
    private final int lineNumber;

    /**
     * The column number where the error occurred, or -1 if unknown.
     */
    private final int columnNumber;

    /**
     * Create an exception with a message.
     *
     * @param message the error message
     */
    public InterpreterException(String message) {
        super(message);
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    /**
     * Create an exception wrapping another exception.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public InterpreterException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    /**
     * Create an exception with location information.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @param lineNumber the line number where error occurred
     * @param columnNumber the column number where error occurred
     */
    public InterpreterException(String message, Throwable cause, int lineNumber, int columnNumber) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Get the line number where the error occurred.
     *
     * @return the line number, or -1 if unknown
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the column number where the error occurred.
     *
     * @return the column number, or -1 if unknown
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Check if location information is available.
     *
     * @return true if line/column information is available
     */
    public boolean hasLocation() {
        return lineNumber >= 0 || columnNumber >= 0;
    }

    @Override
    public String toString() {
        if (hasLocation()) {
            return String.format("%s (line %d, col %d)", getMessage(), lineNumber, columnNumber);
        }
        return getMessage();
    }

}
