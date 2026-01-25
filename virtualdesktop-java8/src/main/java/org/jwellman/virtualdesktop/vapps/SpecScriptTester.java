package org.jwellman.virtualdesktop.vapps;

import org.jwellman.scripttester.ScriptTesterPanel;

/**
 * Script Tester vapp - Step-by-step BeanShell script execution with visual feedback.
 *
 * <p>This is the native Java implementation of the Script Tester tool,
 * providing a UI for debugging and stepping through BeanShell scripts
 * one line at a time.</p>
 *
 * @author Rick Wellman
 */
public class SpecScriptTester extends VirtualAppSpec {

    private ScriptTesterPanel panel;

    public SpecScriptTester() {
        this.height = 400;
        this.width = 600;

        panel = new ScriptTesterPanel();

        // Wire up output to System.out (could be enhanced to use a console)
        panel.setOutputListener(new ScriptTesterPanel.OutputListener() {
            @Override
            public void print(String message) {
                System.out.println(message);
            }
        });

        this.setTitle("Script Tester");
        this.setContent(this.createDefaultContent(panel));
    }

    /**
     * Get the underlying ScriptTesterPanel for programmatic access.
     *
     * @return the panel
     */
    public ScriptTesterPanel getPanel() {
        return panel;
    }

    /**
     * Load a script from text.
     *
     * @param scriptText the script to load
     */
    public void loadScript(String scriptText) {
        panel.loadScriptText(scriptText);
    }
}
