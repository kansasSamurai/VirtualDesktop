package org.jwellman.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * Makes any {@link JComponent} a drag source that wraps its payload in an
 * {@link EmulatorPayload} for recognition by {@link SmartTransferHandler}.
 *
 * The {@code payloadSupplier} is evaluated lazily at drag-start so the
 * component always hands over its current state, not a stale snapshot.
 *
 * Usage:
 * <pre>
 *   SmartDragSource.makeDraggable(myLabel, () -> myDataObject);
 * </pre>
 *
 * Note: {@code makeDraggable} sets a TransferHandler on the component.
 * Do not also set a separate TransferHandler for dropping on the same
 * component — use a parent/child split instead (drag source on child,
 * drop target on parent).
 */
public class SmartDragSource implements Transferable {

    private static final DataFlavor[] FLAVORS = { EmulatorPayload.FLAVOR };

    private final Supplier<Object> payloadSupplier;

    private SmartDragSource(Supplier<Object> payloadSupplier) {
        this.payloadSupplier = payloadSupplier;
    }

    /**
     * Installs drag capability on {@code component} using {@code ACTION_MOVE}.
     */
    public static void makeDraggable(JComponent component, Supplier<Object> payloadSupplier) {
        makeDraggable(component, payloadSupplier, DnDConstants.ACTION_MOVE);
    }

    /**
     * Installs drag capability on {@code component} with an explicit DnD action
     * (e.g. {@code DnDConstants.ACTION_COPY}, {@code ACTION_MOVE}, {@code ACTION_LINK}).
     */
    public static void makeDraggable(JComponent component, Supplier<Object> payloadSupplier, final int action) {
        final SmartDragSource source = new SmartDragSource(payloadSupplier);

        component.setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getSourceActions(JComponent c) {
                return action;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                return source;
            }
        });

        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JComponent c = (JComponent) e.getSource();
                TransferHandler handler = c.getTransferHandler();
                handler.exportAsDrag(c, e, action);
            }
        });
    }

    // --- Transferable ---

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return EmulatorPayload.FLAVOR.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return new EmulatorPayload(payloadSupplier.get());
    }

}
