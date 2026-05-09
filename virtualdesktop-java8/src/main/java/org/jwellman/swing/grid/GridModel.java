package org.jwellman.swing.grid;

import java.util.List;

public interface GridModel {
    int getRowCount();
    GridRow getRow(int index);
    List<ColumnDef> getColumns();
    void addGridModelListener(GridModelListener l);
    void removeGridModelListener(GridModelListener l);
}
