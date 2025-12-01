package org.jwellman.virtualdesktop.vapps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jwellman.jcx.SystemCommandExecutor;

/**
 * VirtualAppSpec for launching external applications.
 * This class represents an external application that should be
 * launched as a separate process rather than as a JInternalFrame.
 *
 * @author Rick Wellman
 */
public class ExternalAppSpec extends VirtualAppSpec implements LaunchAware {

    private String command;
    private String workingDirectory;
    private boolean waitForCompletion;

    /**
     * Creates an external app spec with simple command execution
     * @param title the display name for the application
     * @param command the command line to execute
     */
    public ExternalAppSpec(String title, String command) {
        this(title, command, null, false);
    }

    /**
     * Creates an external app spec with full configuration
     * @param title the display name for the application
     * @param command the command line to execute
     * @param workingDir the working directory (null for current directory)
     * @param wait whether to wait for the process to complete
     */
    public ExternalAppSpec(String title, String command, String workingDir, boolean wait) {
        super();

        this.setTitle(title);
        this.command = command;
        this.workingDirectory = workingDir;
        this.waitForCompletion = wait;

        // External apps don't create internal frames
        // this was created by claude but I believe it is un-necessary and misunderstood use of the property
        // this.internalFrameProvider = false;

        this.setContent(this.createContent());
    }

    private JPanel createContent() {
        final JPanel content = new JPanel();

        content.add(new JLabel("Command: " + this.command));
        return content;
    }

    /**
     * Launches the external application as a separate process
     * @throws IOException if the process cannot be started
     */
    public void launch() throws IOException {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalStateException("Command cannot be null or empty");
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                launchWindows();
            } else {
                launchUnix();
            }
        } catch (Exception ex) {
            System.err.println("Failed to launch external app: " + getTitle());
            ex.printStackTrace();
            throw new IOException("Failed to launch: " + command, ex);
        }
    }

    private void launchWindows() throws Exception {
        List<String> commands = new ArrayList<>();
        commands.add("cmd.exe");
        commands.add("/C");
        commands.add(command);

        File workingDir = workingDirectory != null ? new File(workingDirectory) : null;

        SystemCommandExecutor executor = new SystemCommandExecutor(
            commands, null, null, workingDir);

        if (waitForCompletion) {
            int exitCode = executor.executeCommand();
            System.out.println("External app '" + getTitle() + "' exited with code: " + exitCode);
        } else {
            // Execute in background thread
            new Thread(() -> {
                try {
                    int exitCode = executor.executeCommand();
                    System.out.println("External app '" + getTitle() + "' exited with code: " + exitCode);
                } catch (Exception e) {
                    System.err.println("Error executing external app: " + getTitle());
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void launchUnix() throws IOException {
        File workingDir = workingDirectory != null ? new File(workingDirectory) : null;

        if (waitForCompletion) {
            Process process = Runtime.getRuntime().exec(command, null, workingDir);
            try {
                int exitCode = process.waitFor();
                System.out.println("External app '" + getTitle() + "' exited with code: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while waiting for external app: " + getTitle());
            }
        } else {
            // Fire and forget
            Runtime.getRuntime().exec(command, null, workingDir);
        }
    }

    /**
     * @return the command line
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command line to set
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
     * @return whether to wait for process completion
     */
    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    /**
     * @param waitForCompletion whether to wait for process completion
     */
    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    /**
     * External apps should not create dockable content
     */
    @Override
    public boolean isDockable() {
        return false;
    }
}
