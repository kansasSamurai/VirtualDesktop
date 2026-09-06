package org.jwellman.demo.chess;

import javax.swing.JCheckBox;

public class Options {

    // Chooser - programmatic access to user interface objects 

    public class Chooser {
        public final JCheckBox FLIP_BOARD = new JCheckBox("Flip Board");
        public final JCheckBox SHOW_CONTROL_BADGES = new JCheckBox("Show Control Badges");
        public final JCheckBox SHOW_SQUARE_STRENGTH = new JCheckBox("Show Square Strength");
        public final JCheckBox IGNORE_WHITE_KING = new JCheckBox("Ignore King (White)");
        public final JCheckBox IGNORE_BLACK_KING = new JCheckBox("Ignore King (Black)");
    }

    // ================================================================

    public Chooser Chooser = new Chooser();

    public Options() {
        // Any boolean defaults to true must be set here
        Chooser.SHOW_CONTROL_BADGES.setSelected(true);
        Chooser.SHOW_SQUARE_STRENGTH.setSelected(true);
    }

    public boolean showSquareStrength() {
        return Chooser.SHOW_SQUARE_STRENGTH.isSelected();
    }

    public boolean isFlipped() {
        return Chooser.FLIP_BOARD.isSelected();
    }

    public boolean showControlBadges() {
        return Chooser.SHOW_CONTROL_BADGES.isSelected();
    }

    public boolean ignoreWhiteKing() {
        return Chooser.IGNORE_WHITE_KING.isSelected();
    }

    public boolean ignoreBlackKing() {
        return Chooser.IGNORE_BLACK_KING.isSelected();
    }

}
