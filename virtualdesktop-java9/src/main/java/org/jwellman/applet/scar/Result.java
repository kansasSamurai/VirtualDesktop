package org.jwellman.applet.scar;

import javax.swing.JTextArea;

public class Result extends JTextArea {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private Scar mediator;

    public Result(Scar m) {
        super("substitution-reult-goes-here");
        this.setLineWrap(true);
        this.setWrapStyleWord(true);

        this.mediator = m;
    }
}
