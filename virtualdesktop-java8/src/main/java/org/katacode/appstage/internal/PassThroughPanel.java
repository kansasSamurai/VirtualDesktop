package org.katacode.appstage.internal;

import java.awt.Component;

import javax.swing.JPanel;

/**
 * Full-size overlay panel that ignores hits outside its children so lower
 * layers remain interactive.
 */
@SuppressWarnings("serial")
public class PassThroughPanel extends JPanel {

    public PassThroughPanel() {
        super(null);
        setOpaque(false);
    }

    @Override
    public boolean contains(int x, int y) {
        Component[] kids = getComponents();
        for (int i = 0; i < kids.length; i++) {
            Component c = kids[i];
            if (c.isVisible() && c.getBounds().contains(x, y)) {
                return true;
            }
        }
        return false;
    }
}
