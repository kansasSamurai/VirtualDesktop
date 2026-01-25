package org.jwellman.scripttester;

/**
 * Data model for each script line.
 *
 * @author Rick Wellman
 */
public class ScriptLine {

    private int lineNumber;
    private String text;
    private String status;
    private String errorMessage;

    public ScriptLine(int lineNumber, String text) {
        this.lineNumber = lineNumber;
        this.text = text;
        this.status = Status.PENDING;
        this.errorMessage = null;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return lineNumber + ": " + text + " [" + status + "]";
    }
}
