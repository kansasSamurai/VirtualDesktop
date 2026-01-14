package org.jwellman.applet.scar;

import javax.swing.JButton;

public class Grep extends JButton implements Command {

    private static final long serialVersionUID = 1L;

    private Scar mediator;

    public Grep(Scar m) {
        super("GREP");

        this.mediator = m;
    }

    public void execute() {
        mediator.grep();
    }

}
