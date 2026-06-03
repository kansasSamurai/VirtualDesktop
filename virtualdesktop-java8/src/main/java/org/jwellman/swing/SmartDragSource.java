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
 * <p><b>Transport scope:</b> transfers are JVM-local only. {@link EmulatorPayload#FLAVOR}
 * uses {@code application/x-java-jvm-local-objectref}, so the OS never sees the
 * payload and no serialization occurs. Dragging out to a host OS application
 * (e.g. Windows Explorer) is not supported by this class; see
 * {@code docs/features/smartdrag/DESIGN.md} for the extension strategy.
 *
 * <p><b>One TransferHandler per component:</b> {@code makeDraggable} sets a
 * TransferHandler on the component for export. Do not also call
 * {@code setTransferHandler(new SmartTransferHandler(...))} on the same component —
 * the second call silently overwrites the first. Use a parent/child split instead:
 * drag source on the child, drop target on the parent.
 *
 * Usage:
 * <pre>
 *   SmartDragSource.makeDraggable(childLabel, () -> myDataObject);
 *   parentPanel.setTransferHandler(new SmartTransferHandler(this::onDrop));
 * </pre>
 */
public class SmartDragSource implements Transferable {

    private static final DataFlavor[] FLAVORS = { EmulatorPayload.FLAVOR };

    private final Supplier<Object> payloadSupplier;

    private SmartDragSource(Supplier<Object> payloadSupplier) {
        this.payloadSupplier = payloadSupplier;
    }

    /**
     * Creates a {@link Transferable} that wraps the lazy payload in an
     * {@link EmulatorPayload}. Package-private: the intended entry point for
     * callers is {@link SmartTransferHandler}, which calls this internally from
     * {@code createTransferable()}.
     */
    static Transferable forPayload(Supplier<Object> payloadSupplier) {
        return new SmartDragSource(payloadSupplier);
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
