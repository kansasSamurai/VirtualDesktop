package org.jwellman.swing.grid;

import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;

/** Default footer cell: blank JLabel. Override per-column to show aggregates. */
public class DefaultFooterCellRenderer implements FooterCellRenderer {

    @Override
    public JComponent render(ColumnDef col, List<GridRow> pageRows, GridModel model) {
        JLabel lbl = new JLabel();
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return lbl;
    }
}
