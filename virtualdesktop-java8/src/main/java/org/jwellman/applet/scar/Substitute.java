package org.jwellman.applet.scar;

import javax.swing.JButton;

public class Substitute extends JButton implements Command {

    private static final long serialVersionUID = 1L;

    private Scar mediator;

    public Substitute(Scar m) {
        super("Substitute");

        this.mediator = m;
    }

    public void execute() {
        mediator.substitute();
    }

}
