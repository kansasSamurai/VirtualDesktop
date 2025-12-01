package org.jwellman.virtualdesktop.vapps;

/**
 * Configuration class for defining external applications.
 * Used for JSON deserialization of external app definitions.
 *
 * @author Rick Wellman
 */
public class ExternalAppConfig {

    private String name;
    private String command;
    private String icon;
    private String workingDir;
    private boolean desktopOnly = true;
    private boolean waitForCompletion = false;

    // Default constructor required for Jackson
    public ExternalAppConfig() {
    }

    public ExternalAppConfig(String name, String command, String icon) {
        this.name = name;
        this.command = command;
        this.icon = icon;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return the workingDir
     */
    public String getWorkingDir() {
        return workingDir;
    }

    /**
     * @param workingDir the workingDir to set
     */
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * @return the desktopOnly
     */
    public boolean isDesktopOnly() {
        return desktopOnly;
    }

    /**
     * @param desktopOnly the desktopOnly to set
     */
    public void setDesktopOnly(boolean desktopOnly) {
        this.desktopOnly = desktopOnly;
    }

    /**
     * @return the waitForCompletion
     */
    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    /**
     * @param waitForCompletion the waitForCompletion to set
     */
    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }
}
