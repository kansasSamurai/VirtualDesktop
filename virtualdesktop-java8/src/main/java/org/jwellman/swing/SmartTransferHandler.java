package org.jwellman.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;
import javax.swing.TransferHandler;

/**
 * A reusable drop target that accepts either internal {@link EmulatorPayload}
 * transfers or native OS file drops and hands the unwrapped data to a
 * caller-supplied {@link DropAction} lambda.
 *
 * Usage:
 * <pre>
 *   component.setTransferHandler(new SmartTransferHandler(data -> {
 *       if (data instanceof MyType) { ... }
 *   }));
 * </pre>
 */
public class SmartTransferHandler extends TransferHandler {

    private static final long serialVersionUID = 1L;

    @FunctionalInterface
    public interface DropAction {
        void onDrop(Object data);
    }

    private final DropAction dropAction;

    public SmartTransferHandler(DropAction dropAction) {
        this.dropAction = dropAction;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(EmulatorPayload.FLAVOR)
            || support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
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
