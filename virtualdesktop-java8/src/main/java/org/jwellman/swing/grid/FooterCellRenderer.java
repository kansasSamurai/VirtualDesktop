package org.jwellman.swing.grid;

import java.util.List;
import javax.swing.JComponent;

/**
 * Renders a single cell in the SmartGrid footer row.
 *
 * @param col       the column being rendered
 * @param pageRows  the rows currently visible (current page); use for page-level aggregates
 * @param model     the full model; use for global aggregates across all rows
 */
public interface FooterCellRenderer {
    JComponent render(ColumnDef col, List<GridRow> pageRows, GridModel model);
}
