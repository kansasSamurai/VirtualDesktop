package org.jwellman.applet.scar;

import javax.swing.JPanel;
import javax.swing.JTextField;

public class Field extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTextField textfield;

    public Field(JTextField tf) {
        super();

        this.textfield = tf;
        this.add(tf);
    }

    public JTextField getTextfield() {
        return textfield;
    }

    public void setTextfield(JTextField textfield) {
        this.textfield = textfield;
    }

}
