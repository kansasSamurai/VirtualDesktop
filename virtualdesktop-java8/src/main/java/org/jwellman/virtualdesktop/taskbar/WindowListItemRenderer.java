package org.jwellman.virtualdesktop.taskbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import org.jwellman.virtualdesktop.state.model.FrameState;

/**
 * Custom renderer for WindowListItem in the JList-based window list view.
 *
 * Renders single windows and groups differently:
 * - Single windows show title and icon
 * - Groups show tool type with count badge
 * - Minimized windows shown with italic text
 * - Docking indicators shown with text markers and colors
 *
 * @author rwellman
 */
@SuppressWarnings("serial")
public class WindowListItemRenderer extends DefaultListCellRenderer {

    private static final Color MINIMIZED_COLOR  = Color.GRAY;
    private static final Color GROUP_COLOR      = new Color(70, 130, 180);  // Steel blue
    private static final Color DOCKED_OUT_COLOR = new Color(255, 140,   0); // Dark orange
    private static final Color HAS_EXTRA_COLOR  = new Color( 50, 205,  50); // Lime green
    private static final Color MIXED_COLOR      = new Color(186,  85, 211); // Medium orchid

    private static final String MARKER_DOCKED_OUT = " ←"; // Left arrow
    private static final String MARKER_HAS_EXTRA  = " →"; // Right arrow
    private static final String MARKER_MIXED      = " ⇄"; // Left-right arrows

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        WindowListItem item = (WindowListItem) value;

        String displayText = buildDisplayText(item);

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, displayText, index, isSelected, cellHasFocus);

        if (item.getIcon() != null) {
            label.setIcon(item.getIcon());
        }

        if (item.isGroup()) {
            if (!isSelected) {
                label.setForeground(getGroupColor(item));
            }
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        } else if (item.getFrameState() == FrameState.MINIMIZED) {
            if (!isSelected) {
                label.setForeground(getDockingColor(item, MINIMIZED_COLOR));
            }
            label.setFont(label.getFont().deriveFont(Font.ITALIC));
        } else {
            if (!isSelected) {
                Color dockingColor = getDockingColor(item, null);
                if (dockingColor != null) {
                    label.setForeground(dockingColor);
                }
            }
        }

        return label;
    }

    private String buildDisplayText(WindowListItem item) {
        String title = item.getTitle();
        DockingIndicator indicator = item.getDockingIndicator();

        if (indicator == null || indicator == DockingIndicator.NORMAL) {
            return title;
        }

        switch (indicator) {
            case DOCKED_OUT:        return title + MARKER_DOCKED_OUT;
            case HAS_EXTRA_CONTENT: return title + MARKER_HAS_EXTRA;
            case MIXED:             return title + MARKER_MIXED;
            default:                return title;
        }
    }

    private Color getDockingColor(WindowListItem item, Color defaultColor) {
        DockingIndicator indicator = item.getDockingIndicator();

        if (indicator == null || indicator == DockingIndicator.NORMAL) {
            return defaultColor;
        }

        switch (indicator) {
            case DOCKED_OUT:        return DOCKED_OUT_COLOR;
            case HAS_EXTRA_CONTENT: return HAS_EXTRA_COLOR;
            case MIXED:             return MIXED_COLOR;
            default:                return defaultColor;
        }
    }

    private Color getGroupColor(WindowListItem item) {
        Color dockingColor = getDockingColor(item, null);
        return dockingColor != null ? dockingColor : GROUP_COLOR;
    }

}
