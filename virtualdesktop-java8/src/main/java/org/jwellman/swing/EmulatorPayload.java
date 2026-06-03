package org.jwellman.swing;

import java.awt.datatransfer.DataFlavor;

/**
 * Generic envelope for dragging objects across the virtual desktop.
 *
 * The primary value of this class is {@link #FLAVOR}: a shared DataFlavor
 * that lets internal components recognize one another's transfers without
 * touching DataFlavor or Transferable directly. The {@link #getData} helper
 * is a convenience cast — callers still need instanceof checks on the result.
 *
 * FLAVOR uses javaJVMLocalObjectMimeType so the payload is passed by reference
 * within the JVM. Do NOT use DataFlavor(Class, String) here — that constructor
 * produces application/x-java-serialized-object, which forces serialization and
 * breaks payloads that hold non-serializable references (e.g. Swing components).
 */
public class EmulatorPayload {

    public static final DataFlavor FLAVOR = new DataFlavor(
        DataFlavor.javaJVMLocalObjectMimeType
            + ";class=\"" 
            + EmulatorPayload.class.getName() 
            + "\"",
        "Emulator Desktop Object"
    );

    private final Object data;

    public EmulatorPayload(Object data) {
        this.data = data;
    }

    public <T> T getData(Class<T> type) {
        return type.cast(data);
    }

}
