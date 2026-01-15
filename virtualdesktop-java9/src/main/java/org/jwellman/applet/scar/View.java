package org.jwellman.applet.scar;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

public class View extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private Scar mediator;

    public View(Scar m) {
        super(new BorderLayout());

        this.mediator = m;
    }

    public void actionPerformed(ActionEvent e) {
        Command cmd = (Command) e.getSource();
        cmd.execute();
    }

}
