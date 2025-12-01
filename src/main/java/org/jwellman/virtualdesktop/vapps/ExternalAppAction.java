package org.jwellman.virtualdesktop.vapps;

import org.jwellman.virtualdesktop.DesktopManager;

/**
 * Action for launching external applications.
 * Extends DesktopAction but launches external processes instead of internal vapps.
 *
 * @author Rick Wellman
 */
public class ExternalAppAction extends DesktopAction {

    private static final long serialVersionUID = 1L;

    private String command;
    private String workingDirectory;
    private boolean waitForCompletion;

    /**
     * Creates an external app action with simple command execution
     * @param name the display name for the application
     * @param command the command line to execute
     */
    public ExternalAppAction(String name, String command) {
        this(name, command, null, false);
    }

    /**
     * Creates an external app action with full configuration
     * @param name the display name for the application
     * @param command the command line to execute
     * @param workingDir the working directory (null for current directory)
     * @param wait whether to wait for the process to complete
     */
    public ExternalAppAction(String name, String command, String workingDir, boolean wait) {
        super(name);
        this.command = command;
        this.workingDirectory = workingDir;
        this.waitForCompletion = wait;
        this.setDesktopOnly(true); // External apps are typically desktop-only
    }

    /**
     * Overrides the run method to launch external apps instead of internal vapps.
     * Called by actionPerformed() which is inherited from DesktopAction.
     */
    @Override
    public void run() {
        // Create the external app spec and launch it via DesktopManager
        ExternalAppSpec spec = new ExternalAppSpec(
            (String) getValue(NAME),
            command,
            workingDirectory,
            waitForCompletion
        );

        // Set icon if available
        if (getValue(LARGE_ICON_KEY) != null) {
            spec.setIcon((javax.swing.Icon) getValue(LARGE_ICON_KEY));
        }

        // Use DesktopManager to launch (it will detect ExternalAppSpec and launch appropriately)
        DesktopManager.get().createVApp(spec);
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
     * @return the working directory
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * @param workingDirectory the working directory to set
     */
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * @return whether to wait for completion
     */
    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    /**
     * @param waitForCompletion whether to wait for completion
     */
    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }
}
