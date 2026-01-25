package org.jwellman.scripttester;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom cell renderer for the line number column.
 *
 * @author Rick Wellman
 */
public class LineNumberCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final Color COLOR_COMPLETED_BG = new Color(230, 230, 230);
    private static final Color COLOR_CURRENT_BG = new Color(255, 255, 180);
    private static final Color COLOR_ERROR_BG = new Color(255, 200, 200);
    private static final Color COLOR_SKIPPED_BG = new Color(240, 240, 240);

    public LineNumberCellRenderer() {
        setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column);

        // Get the status from column 0
        String status = (String) table.getValueAt(row, 0);

        label.setHorizontalAlignment(SwingConstants.RIGHT);

        if (!isSelected) {
            if (Status.COMPLETED.equals(status)) {
                label.setBackground(COLOR_COMPLETED_BG);
                label.setForeground(Color.DARK_GRAY);
            } else if (Status.CURRENT.equals(status)) {
                label.setBackground(COLOR_CURRENT_BG);
                label.setForeground(Color.BLACK);
            } else if (Status.ERROR.equals(status)) {
                label.setBackground(COLOR_ERROR_BG);
                label.setForeground(Color.BLACK);
            } else if (Status.SKIPPED.equals(status)) {
                label.setBackground(COLOR_SKIPPED_BG);
                label.setForeground(Color.GRAY);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(Color.GRAY);
            }
        }

        return label;
    }
}
