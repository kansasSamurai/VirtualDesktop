package org.jwellman.virtualdesktop.vapps;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.jwellman.standalone.UberHandler;

/**
 * A simple implementation of the UberHandler that can be
 * used for both demonstration and debugging purposes.
 * 
 * @author Rick Wellman
 */
public class SpecUberDragAndDrop extends VirtualAppSpec {

    public SpecUberDragAndDrop() {

        this.setTitle("DragAndDrop Demo");

        final JTextArea jta = new JTextArea(10, 40);
        jta.setLineWrap(true);
        jta.setWrapStyleWord(true);

        final UberHandler uh = new UberHandler();
        jta.setTransferHandler(uh);
        uh.setOutput(jta);

        this.setContent(this.createDefaultContent(new JScrollPane(jta)));

    }
}
