package org.jwellman.swing.grid;

import java.util.EventListener;

public interface GridModelListener extends EventListener {
    void rowsChanged(int firstRow, int lastRow);
    void modelReset();
}
