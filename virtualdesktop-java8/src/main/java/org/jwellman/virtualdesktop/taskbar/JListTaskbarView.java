package org.jwellman.virtualdesktop.taskbar;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * JList-based implementation of TaskbarView.
 *
 * Owns and manages its JList and DefaultListModel internally.
 * Interaction events are translated to TaskbarViewListener callbacks;
 * no action logic lives here.
 */
public class JListTaskbarView implements TaskbarView {

    private final JList<TaskbarItem> list;
    private final DefaultListModel<TaskbarItem> model;
    private TaskbarViewListener listener;

    // Prevents selection-change events fired by setSelectedId() from looping back
    private boolean updatingSelection = false;

    // Recorded on mousePressed so it is available when valueChanged fires
    // (valueChanged is triggered by mousePressed, before mouseClicked)
    private Point lastPressScreen = new Point(0, 0);

    public JListTaskbarView() {
        model = new DefaultListModel<>();
        list = new JList<>(model);
        list.setCellRenderer(new TaskbarItemRenderer());
        wireMouseListener();
        wireSelectionListener();
    }

    // -------------------------------------------------------------------------
    // TaskbarView implementation
    // -------------------------------------------------------------------------

    @Override
    public JComponent getComponent() {
        return list;
    }

    @Override
    public void setItems(List<TaskbarItem> items) {
        model.clear();
        for (TaskbarItem item : items) {
            model.addElement(item);
        }
    }

    @Override
    public void setSelectedId(String toolId) {
        if (toolId == null) {
            if (!list.isSelectionEmpty()) {
                updatingSelection = true;
                list.clearSelection();
                updatingSelection = false;
            }
            return;
        }

        for (int i = 0; i < model.size(); i++) {
            if (toolId.equals(model.get(i).getId())) {
                if (list.getSelectedIndex() != i) {
                    updatingSelection = true;
                    list.setSelectedIndex(i);
                    updatingSelection = false;
                }
                return;
            }
        }
    }

    @Override
    public void setListener(TaskbarViewListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // Internal wiring
    // -------------------------------------------------------------------------

    private void wireSelectionListener() {
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || updatingSelection || listener == null) {
                    return;
                }
                TaskbarItem selected = list.getSelectedValue();
                if (selected == null) {
                    return;
                }
                if (selected.isGroup()) {
                    // Left-clicking a group opens its context popup; use the
                    // screen position captured at mousePressed (which triggered
                    // this selection event) for accurate popup placement.
                    listener.onContextRequested(selected.getId(), true, lastPressScreen);
                } else {
                    listener.onItemSelected(selected.getId(), false);
                }
            }
        });
    }

    private void wireMouseListener() {
        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                // Record position before valueChanged fires (valueChanged is
                // triggered by this same press event).
                lastPressScreen = e.getLocationOnScreen();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (listener == null) {
                    return;
                }
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }
                TaskbarItem item = model.get(index);

                if (e.getButton() == MouseEvent.BUTTON3) {
                    listener.onContextRequested(item.getId(), item.isGroup(),
                            e.getLocationOnScreen());
                } else if (e.getClickCount() == 2) {
                    listener.onItemActivated(item.getId(), item.isGroup());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Fallback popup position — used when no click coordinates are available
    // (e.g., keyboard-driven selection of a group item)
    // -------------------------------------------------------------------------

    /**
     * Returns a screen-coordinate Point suitable for anchoring a popup next to
     * the currently selected cell. Falls back to (0,0) if no cell is selected.
     */
    Point getSelectedCellScreenPoint() {
        int index = list.getSelectedIndex();
        if (index < 0) {
            return new Point(0, 0);
        }
        Rectangle bounds = list.getCellBounds(index, index);
        if (bounds == null) {
            return new Point(0, 0);
        }
        Point pt = new Point(bounds.x + bounds.width, bounds.y);
        javax.swing.SwingUtilities.convertPointToScreen(pt, list);
        return pt;
    }

}
