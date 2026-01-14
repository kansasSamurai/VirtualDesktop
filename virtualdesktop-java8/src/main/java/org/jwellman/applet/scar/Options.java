package org.jwellman.applet.scar;

import javax.swing.JCheckBox;

/**
 * Application options.
 * <p>
 * There is not currently a mechanism for persistence.
 * (the number of options is small so this is not currently a limitation)
 * 
 * @author rwellman
 *
 */
public class Options {

    // Chooser - programmatic access to user interface objects 

    class Chooser {
        public final JCheckBox REMOVE_XML = new JCheckBox("Remove XML");
        public final JCheckBox REMOVE_BLANKLINES = new JCheckBox("Remove Blank Lines");
        public final JCheckBox AUTO_COPY = new JCheckBox("Auto Copy Result");
    }

    // ================================================================

    public Chooser Chooser = new Chooser();

    public Options() {
        Chooser.AUTO_COPY.setSelected(true);
        Chooser.REMOVE_XML.setSelected(true);
        Chooser.REMOVE_BLANKLINES.setSelected(true);
    }

    public boolean removeXML() {
        return Chooser.REMOVE_XML.isSelected();
    }

    public boolean removeBlankLines() {
        return Chooser.REMOVE_BLANKLINES.isSelected();
    }

    public boolean autoCopyResult() {
        return Chooser.AUTO_COPY.isSelected();
    }

}
