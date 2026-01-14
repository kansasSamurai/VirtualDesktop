package org.jwellman.applet.scar;

import javax.swing.JButton;

public class Parse extends JButton implements Command {

    private static final long serialVersionUID = 1L;

    private Scar mediator;

    public Parse(Scar m) {
        super("Parse");

        this.mediator = m;
    }

    public void execute() {
        mediator.parse();
    }

}
