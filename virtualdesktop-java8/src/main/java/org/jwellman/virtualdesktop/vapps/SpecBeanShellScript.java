package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import bsh.This;
import org.jwellman.virtualdesktop.bsh.BeanShellService;

/**
 * A VirtualAppSpec that loads its content from a BeanShell script.
 *
 * Scripts can return content in two ways:
 * 1. Simple: Return a JComponent directly
 * 2. Advanced: Return a BeanShell "this" object with lifecycle methods
 *
 * Advanced scripts can implement these optional methods:
 * - getContent() - returns the JComponent to display
 * - configure(Map attrs) - called with configuration attributes
 * - launch() - called when the tool is displayed
 *
 * Example simple script:
 * <pre>
 * import javax.swing.*;
 * panel = new JPanel();
 * panel.add(new JLabel("Hello from script!"));
 * return panel;
 * </pre>
 *
 * Example advanced script (recommended for lifecycle support):
 * <pre>
 * notepad() {
 *     textArea = new JTextArea();
 *     panel = new JPanel(new BorderLayout());
 *     panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
 *
 *     getContent() { return panel; }
 *     configure(attrs) { ... }
 *     launch() { textArea.requestFocus(); }
 *
 *     return this;
 * }
 * global.notepad = notepad();
 * return global.notepad;  // Return "this" for lifecycle method support
 * </pre>
 *
 * @author rwellman
 */
public class SpecBeanShellScript extends VirtualAppSpec
        implements Configurable, LaunchAware {

    private static final Logger LOG = Logger.getLogger(SpecBeanShellScript.class.getName());

    private String scriptPath;
    private This scriptObject;
    private Map<String, String> attrs;

    public SpecBeanShellScript() {
        super();
        this.setTitle("Scripted Tool");
    }

    @Override
    public void configure(Map<String, String> attrs) {
        this.attrs = attrs;
        if (attrs.containsKey("scriptPath")) {
            this.scriptPath = attrs.get("scriptPath");
        }
        if (attrs.containsKey("title")) {
            this.setTitle(attrs.get("title"));
        }

        executeScript();

        // Pass attrs to script's configure() if it exists
        if (scriptObject != null) {
            invokeOptionalMethod("configure", attrs);
        }
    }

    private void executeScript() {
        if (scriptPath == null || scriptPath.isEmpty()) {
            this.setContent(createErrorPanel("No scriptPath specified"));
            return;
        }

        try {
            BeanShellService bsh = BeanShellService.get();

            // Execute script using shared interpreter
            Object result = bsh.source(scriptPath);

            if (result instanceof JComponent) {
                // Simple script returned a component directly
                this.setContent(createDefaultContent((JComponent) result));
            } else if (result instanceof This) {
                // Advanced script returned a "this" object
                scriptObject = (This) result;
                Object content = scriptObject.invokeMethod("getContent", new Object[]{});
                if (content instanceof JComponent) {
                    this.setContent(createDefaultContent((JComponent) content));
                } else {
                    this.setContent(createErrorPanel("Script getContent() did not return a JComponent"));
                }
            } else if (result == null) {
                this.setContent(createErrorPanel("Script returned null"));
            } else {
                this.setContent(createErrorPanel("Script returned unexpected type: " + result.getClass().getName()));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Script execution failed: " + scriptPath, e);
            this.setContent(createErrorPanel(e));
        }
    }

    @Override
    public void launch() throws Exception {
        invokeOptionalMethod("launch");
    }

    /**
     * Invoke an optional method on the script object.
     * Silently ignores if the method doesn't exist.
     */
    private void invokeOptionalMethod(String method, Object... args) {
        if (scriptObject != null) {
            try {
                scriptObject.invokeMethod(method, args);
            } catch (Exception e) {
                // Method is optional - log at fine level only
                LOG.log(Level.FINE, "Optional method not found or failed: " + method, e);
            }
        }
    }

    /**
     * Create an error panel displaying an exception.
     */
    private JPanel createErrorPanel(Exception e) {
        String message = e.getClass().getSimpleName() + ": " + e.getMessage();
        return createErrorPanel(message);
    }

    /**
     * Create an error panel displaying a message.
     */
    private JPanel createErrorPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(255, 240, 240));

        JLabel titleLabel = new JLabel("Script Error");
        titleLabel.setForeground(new Color(180, 0, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(panel.getBackground());
        messageArea.setForeground(Color.DARK_GRAY);
        panel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        if (scriptPath != null) {
            JLabel pathLabel = new JLabel("Script: " + scriptPath);
            pathLabel.setForeground(Color.GRAY);
            panel.add(pathLabel, BorderLayout.SOUTH);
        }

        return panel;
    }

    /**
     * Get the script path.
     */
    public String getScriptPath() {
        return scriptPath;
    }

}
