package org.jwellman.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * A MouseAdapter that initiates a drag operation when the mouse is pressed.
 * Used with TransferHandler to enable drag-and-drop from a component.
 *
 * <p>Usage:
 * <pre>
 * component.setTransferHandler(new TransferHandler("propertyName"));
 * component.addMouseListener(new DragInitiator(component));
 * </pre>
 *
 * @author rwellman
 */
public class DragInitiator extends MouseAdapter {

    private final JComponent component;

    public DragInitiator(JComponent component) {
        this.component = component;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        TransferHandler handler = component.getTransferHandler();
        handler.exportAsDrag(component, e, TransferHandler.COPY);
    }

}
