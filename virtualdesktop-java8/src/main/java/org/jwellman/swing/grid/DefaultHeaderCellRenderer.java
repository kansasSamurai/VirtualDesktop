package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Default header cell: bold white JLabel with optional sort indicator.
 * Sortable columns show a HAND cursor and a ▲/▼ suffix when sorted.
 */
public class DefaultHeaderCellRenderer implements HeaderCellRenderer {

    @Override
    public JComponent render(ColumnDef col, SortOrder sortOrder) {
        String text = col.getHeader();
        if (sortOrder == SortOrder.ASCENDING)  text += " ▲"; // ▲
        if (sortOrder == SortOrder.DESCENDING) text += " ▼"; // ▼

        JLabel lbl = new JLabel(text);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        if (col.isSortable()) {
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        return lbl;
    }
}
