package org.jwellman.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * A unified drag-and-drop handler that can cover export, import, or both,
 * all wired via lambdas so no subclassing is required.
 *
 * <h3>Import-only</h3>
 * <pre>
 *   component.setTransferHandler(new SmartTransferHandler(data -> {
 *       if (data instanceof MyType) { ... }
 *   }));
 * </pre>
 *
 * <h3>Export + import (single component owns both directions)</h3>
 * <pre>
 *   component.setTransferHandler(new SmartTransferHandler(
 *       () -> payloadObject,          // evaluated lazily at drag-start
 *       data -> handleDrop(data),     // called on successful drop
 *       () -> cleanup()               // called when drag ends (success or cancel)
 *   ));
 * </pre>
 * Child components that initiate the drag should call:
 * <pre>
 *   parentPanel.getTransferHandler().exportAsDrag(parentPanel, mouseEvent, TransferHandler.MOVE);
 * </pre>
 * This keeps the TransferHandler on the parent so drops anywhere inside the
 * component — including over child widgets — are handled consistently.
 */
public class SmartTransferHandler extends TransferHandler {

    private static final long serialVersionUID = 1L;

    @FunctionalInterface
    public interface DropAction {
        void onDrop(Object data);
    }

    private final Supplier<Object> payloadSupplier;
    private final DropAction dropAction;
    private final Runnable onExportDone;

    /** Import-only: accepts drops, no drag-out support. */
    public SmartTransferHandler(DropAction dropAction) {
        this(null, dropAction, null);
    }

    /**
     * Export + import: the component can both initiate drags and accept drops.
     *
     * @param payloadSupplier evaluated lazily at drag-start; return {@code null}
     *                        to suppress the drag (e.g. when nothing is selected)
     * @param dropAction      called with the unwrapped payload on a successful drop
     * @param onExportDone    called when the drag ends regardless of outcome;
     *                        use to clear any "dragging" state. May be {@code null}.
     */
    public SmartTransferHandler(Supplier<Object> payloadSupplier, DropAction dropAction, Runnable onExportDone) {
        this.payloadSupplier = payloadSupplier;
        this.dropAction      = dropAction;
        this.onExportDone    = onExportDone;
    }

    // ── Export side ──────────────────────────────────────────────────────────

    @Override
    public int getSourceActions(JComponent c) {
        return payloadSupplier != null ? MOVE : NONE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (payloadSupplier == null) {
            return null;
        }
        return SmartDragSource.forPayload(payloadSupplier);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if (onExportDone != null) {
            onExportDone.run();
        }
    }

    // ── Import side ──────────────────────────────────────────────────────────

    @Override
    public boolean canImport(TransferSupport support) {
        return dropAction != null && (
            support.isDataFlavorSupported(EmulatorPayload.FLAVOR)
            || support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        );
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        Transferable t = support.getTransferable();
        try {
            if (support.isDataFlavorSupported(EmulatorPayload.FLAVOR)) {
                EmulatorPayload payload = (EmulatorPayload) t.getTransferData(EmulatorPayload.FLAVOR);
                dropAction.onDrop(payload.getData(Object.class));
                return true;
            }

            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                if (!files.isEmpty()) {
                    dropAction.onDrop(files.get(0));
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
