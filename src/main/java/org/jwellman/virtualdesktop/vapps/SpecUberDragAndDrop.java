package org.jwellman.virtualdesktop.vapps;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.jwellman.standalone.UberHandler;

/**
 *
 * @author Rick Wellman
 */
public class SpecUberDragAndDrop extends VirtualAppSpec {

    public SpecUberDragAndDrop() {

        this.setTitle("DragAndDrop Demo");

        final UberHandler uh = new UberHandler();
        final JTextArea jta = new JTextArea();
        jta.setTransferHandler(uh);
        uh.setOutput(jta);

        this.setContent(this.createDefaultContent(new JScrollPane(jta)));

    }
}
