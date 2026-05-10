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
 * Default header cell renderer.
 *
 * Layout: [column name + sort arrow — CENTER] [rank number — EAST]
 *
 * The sort arrow (▲ / ▼ / ▫) is placed directly after the column name so the
 * user immediately sees whether and how the column is sorted. The rank number
 * (1, 2, 3…) is secondary information and lives in the east corner, only visible
 * during multi-column sort. Single-column sort shows just the arrow, no rank.
 */
public class DefaultHeaderCellRenderer implements HeaderCellRenderer {

    @Override
    public JComponent render(ColumnDef col, SortOrder sortOrder, int sortRank) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setOpaque(false); // let the parent header panel's background show through

        // Column name + sort arrow: primary, directly associated with the column
        JLabel nameLabel = new JLabel(buildNameText(col.getHeader(), sortOrder, col.isSortable()));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 4));

        // Rank number: secondary, only shown during multi-column sort (rank > 0)
        JLabel rankLabel = new JLabel(sortRank > 0 ? String.valueOf(sortRank) : "");
        rankLabel.setForeground(Color.WHITE);
        rankLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rankLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 6));

        cell.add(nameLabel, BorderLayout.CENTER);
        cell.add(rankLabel, BorderLayout.EAST);

        if (col.isSortable()) {
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        return cell;
    }

    private static String buildNameText(String header, SortOrder order, boolean sortable) {
        if (order == SortOrder.ASCENDING)  return header + " ▲";
        if (order == SortOrder.DESCENDING) return header + " ▼";
        return sortable ? header + " ▫" : header;
    }
}
