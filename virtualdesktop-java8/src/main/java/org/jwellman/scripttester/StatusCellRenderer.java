package org.jwellman.scripttester;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom cell renderer for the status column - shows icons/symbols.
 *
 * @author Rick Wellman
 */
public class StatusCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final Color COLOR_GREEN = new Color(0, 128, 0);
    private static final Color COLOR_BLUE = new Color(0, 0, 200);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(
            table, "", isSelected, hasFocus, row, column);

        String status = (String) value;
        label.setHorizontalAlignment(SwingConstants.CENTER);

        // Use text symbols for status (works across all platforms)
        if (Status.COMPLETED.equals(status)) {
            label.setText("\u2713"); // checkmark
            label.setForeground(COLOR_GREEN);
        } else if (Status.CURRENT.equals(status)) {
            label.setText("\u25B6"); // right-pointing triangle (play)
            label.setForeground(COLOR_BLUE);
        } else if (Status.ERROR.equals(status)) {
            label.setText("\u2717"); // X mark
            label.setForeground(Color.RED);
        } else if (Status.SKIPPED.equals(status)) {
            label.setText("\u2014"); // em dash
            label.setForeground(Color.GRAY);
        } else {
            label.setText(""); // pending - no symbol
            label.setForeground(Color.GRAY);
        }

        return label;
    }
}
