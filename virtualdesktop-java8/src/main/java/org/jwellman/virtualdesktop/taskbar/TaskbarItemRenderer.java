package org.jwellman.virtualdesktop.taskbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import org.jwellman.virtualdesktop.state.model.FrameState;

/**
 * Custom renderer for TaskbarItem in the taskbar JList.
 *
 * Renders single tools and groups differently:
 * - Single tools show title and icon
 * - Groups show tool type with count badge
 * - Minimized tools shown with italic text
 *
 * @author rwellman
 */
@SuppressWarnings("serial")
public class TaskbarItemRenderer extends DefaultListCellRenderer {

    private static final Color MINIMIZED_COLOR = Color.GRAY;
    private static final Color GROUP_COLOR = new Color(70, 130, 180); // Steel blue

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        TaskbarItem item = (TaskbarItem) value;

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, item.getTitle(), index, isSelected, cellHasFocus);

        // Set icon
        if (item.getIcon() != null) {
            label.setIcon(item.getIcon());
        }

        // Style based on state
        if (item.isGroup()) {
            // Group styling
            if (!isSelected) {
                label.setForeground(GROUP_COLOR);
            }
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        } else if (item.getFrameState() == FrameState.MINIMIZED) {
            // Minimized tool styling
            if (!isSelected) {
                label.setForeground(MINIMIZED_COLOR);
            }
            label.setFont(label.getFont().deriveFont(Font.ITALIC));
        }

        return label;
    }

}
