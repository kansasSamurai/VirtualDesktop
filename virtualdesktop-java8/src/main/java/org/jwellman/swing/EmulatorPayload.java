package org.jwellman.swing;

import java.awt.datatransfer.DataFlavor;
import java.io.Serializable;

/**
 * Generic envelope for dragging objects across the virtual desktop.
 *
 * The primary value of this class is {@link #FLAVOR}: a shared DataFlavor
 * that lets internal components recognize one another's transfers without
 * touching DataFlavor or Transferable directly. The {@link #getData} helper
 * is a convenience cast — callers still need instanceof checks on the result.
 */
public class EmulatorPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final DataFlavor FLAVOR =
        new DataFlavor(EmulatorPayload.class, "Emulator Desktop Object");

    private final Object data;

    public EmulatorPayload(Object data) {
        this.data = data;
    }

    public <T> T getData(Class<T> type) {
        return type.cast(data);
    }

}
