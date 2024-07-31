package org.jwellman.applet.scar;

import javax.swing.JTextArea;

public class Template extends JTextArea {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private Scar mediator;

    public Template(Scar m) {
        super("<insert id = \"insert\" parameterType = \"Student\">\r\n"
                + "   INSERT INTO STUDENT1 (NAME, BRANCH, PERCENTAGE, PHONE, EMAIL ) \r\n"
                + "   VALUES (#{name}, #{branch}, #{percentage}, #{phone}, #{email});    \r\n"
                + "</insert>");
        this.setLineWrap(true);
        this.setWrapStyleWord(true);

        this.mediator = m;
    }
}
