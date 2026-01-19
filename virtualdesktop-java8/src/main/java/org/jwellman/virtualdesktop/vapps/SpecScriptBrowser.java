package org.jwellman.virtualdesktop.vapps;

import org.jwellman.swing.jtree.FileNode;
import org.jwellman.swing_applets.FileNavigator;

/**
 * Script Browser vapp that provides a file navigator for browsing and viewing
 * BeanShell scripts and markdown files.
 *
 * Uses FileNavigator with TYPE.TEXT to display a JTree on the left for
 * navigation and a text preview on the right.
 *
 * @author Rick Wellman
 */
public class SpecScriptBrowser extends VirtualAppSpec {

    public SpecScriptBrowser() {
        this.height = 500;
        this.width = 700;

        // Register script file types as viewable text
        FileNode.addTextTypes(".bsh");
        FileNode.addTextTypes(".md");

        // Create navigator pointing to scripts folder
        String scriptsPath = "src/main/resources/org/jwellman/bsh/scripts";
        FileNavigator navigator = new FileNavigator(scriptsPath, FileNavigator.TYPE.TEXT);

        this.setTitle("Script Browser");
        this.setContent(this.createDefaultContent(navigator));
    }

}
