package org.jwellman.applet.scar;

import javax.swing.JCheckBox;

public class Options {

    public Chooser Chooser = new Chooser();

    public Options() {
        Chooser.REMOVE_XML.setSelected(true);
    }

    class Chooser {
        public final JCheckBox REMOVE_XML = new JCheckBox("Remove XML");
    }

    public boolean removeXML() {
        return Chooser.REMOVE_XML.isSelected();
    }
}
