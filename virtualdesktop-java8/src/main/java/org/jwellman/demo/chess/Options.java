package org.jwellman.demo.chess;

import javax.swing.JCheckBox;

public class Options {

    // Chooser - programmatic access to user interface objects 

    public class Chooser {
        public final JCheckBox SHOW_SQUARE_STRENGTH = new JCheckBox("Show Square Strength");
    }

    // ================================================================

    public Chooser Chooser = new Chooser();

    public Options() {
        Chooser.SHOW_SQUARE_STRENGTH.setSelected(true);
    }

    public boolean showSquareStrength() {
        return Chooser.SHOW_SQUARE_STRENGTH.isSelected();
    }

}
