package org.jwellman.swing.grid;

import java.util.List;

public interface GridModel {
    int getRowCount();
    /** Returns the total number of rows regardless of any active filter or tree collapse. */
    int getTotalRowCount();
    GridRow getRow(int index);
    List<ColumnDef> getColumns();
    void addGridModelListener(GridModelListener l);
    void removeGridModelListener(GridModelListener l);
}
