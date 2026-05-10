package org.jwellman.swing.grid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Default header cell: column name in CENTER, sort indicator in EAST of a
 * BorderLayout JPanel. The outer panel is transparent so the header row's
 * background colour shows through.
 *
 * Single sort: shows ▲ or ▼ with no rank number.
 * Multi-column sort: shows 1▲, 2▼ … on each sorted column.
 * Sortable but unsorted: shows ▫ to signal the column is clickable.
 */
public class DefaultHeaderCellRenderer implements HeaderCellRenderer {

    @Override
    public JComponent render(ColumnDef col, SortOrder sortOrder, int sortRank) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setOpaque(false); // let the parent header panel's background show through

        JLabel nameLabel = new JLabel(col.getHeader());
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 4));

        JLabel sortLabel = new JLabel(buildSortText(sortOrder, sortRank, col.isSortable()));
        sortLabel.setForeground(Color.WHITE);
        sortLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        sortLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 6));

        cell.add(nameLabel, BorderLayout.CENTER);
        cell.add(sortLabel, BorderLayout.EAST);

        if (col.isSortable()) {
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        return cell;
    }

    /**
     * Builds the text for the sort indicator label.
     * Rank is shown only when greater than zero (i.e. multi-column sort is active).
     */
    private static String buildSortText(SortOrder order, int rank, boolean sortable) {
        if (order == SortOrder.NONE) {
            return sortable ? "▫" : "";
        }
        String arrow = (order == SortOrder.ASCENDING) ? "▲" : "▼";
        return (rank > 0) ? rank + arrow : arrow;
    }
}
