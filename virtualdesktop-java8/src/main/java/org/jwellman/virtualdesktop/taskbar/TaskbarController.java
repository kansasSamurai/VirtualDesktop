package org.jwellman.virtualdesktop.taskbar;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jwellman.virtualdesktop.DesktopManager;
import org.jwellman.virtualdesktop.VirtualAppFrame;
import org.jwellman.virtualdesktop.state.model.AppState;
import org.jwellman.virtualdesktop.state.model.DockingState;
import org.jwellman.virtualdesktop.state.model.ToolInstance;
import org.jwellman.virtualdesktop.state.model.ToolsState;
import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.store.AppStore;
import org.jwellman.virtualdesktop.state.store.StoreSubscriber;
import org.jwellman.virtualdesktop.state.store.Subscription;

/**
 * Controller that bridges Redux state to the taskbar JList view.
 *
 * Subscribes to the store and updates the JList model when state changes.
 * Handles grouping of tools by type when enabled.
 *
 * @author rwellman
 */
public class TaskbarController implements StoreSubscriber, ListSelectionListener {

    private final JList<TaskbarItem> taskbarList;
    private final DefaultListModel<TaskbarItem> listModel;
    private final Subscription subscription;

    // Cache of toolId -> VirtualAppFrame for icon lookup
    private final Map<String, VirtualAppFrame> frameCache = new HashMap<>();

    // Flag to prevent recursive selection events
    private boolean updatingSelection = false;

    // Track last mouse click location for popup positioning
    private int lastClickX = 0;
    private int lastClickY = 0;

    public TaskbarController(JList<TaskbarItem> taskbarList) {
        this.taskbarList = taskbarList;
        this.listModel = new DefaultListModel<>();
        this.taskbarList.setModel(listModel);
        this.taskbarList.setCellRenderer(new TaskbarItemRenderer());
        this.taskbarList.addListSelectionListener(this);

        // Add mouse listener for tracking click location and handling group clicks
        this.taskbarList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                lastClickX = e.getX();
                lastClickY = e.getY();

                // Handle double-click on group to expand all
                if (e.getClickCount() == 2) {
                    int index = taskbarList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        TaskbarItem item = listModel.get(index);
                        if (item.isGroup()) {
                            activateAllInGroup(item);
                        }
                    }
                }
            }
        });

        // Subscribe to store
        this.subscription = AppStore.get().subscribe(this);

        // Initial render
        onStateChanged(AppStore.get().getState());
    }

    @Override
    public void onStateChanged(AppState state) {
        // Ensure we update on EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onStateChanged(state));
            return;
        }

        updateFrameCache();
        rebuildListModel(state);
        updateSelection(state);
    }

    /**
     * Update the frame cache from DesktopManager's frame list.
     */
    private void updateFrameCache() {
        frameCache.clear();
        for (VirtualAppFrame frame : DesktopManager.get().getFrames()) {
            frameCache.put(frame.getToolId(), frame);
        }
    }

    /**
     * Rebuild the list model from current state.
     */
    private void rebuildListModel(AppState state) {
        ToolsState tools = state.getTools();
        boolean groupingEnabled = state.getTaskbar().isGroupingEnabled();

        listModel.clear();

        if (groupingEnabled) {
            // Build grouped items
            Map<String, List<TaskbarItem>> groups = new HashMap<>();

            for (ToolInstance tool : tools.getAllTools()) {
                String type = tool.getToolType() != null ? tool.getToolType() : "Unknown";
                TaskbarItem item = createTaskbarItem(tool);

                if (!groups.containsKey(type)) {
                    groups.put(type, new ArrayList<TaskbarItem>());
                }
                groups.get(type).add(item);
            }

            // Add groups to model
            for (Map.Entry<String, List<TaskbarItem>> entry : groups.entrySet()) {
                List<TaskbarItem> items = entry.getValue();
                if (items.size() == 1) {
                    // Single item - don't group
                    listModel.addElement(items.get(0));
                } else {
                    // Multiple items - create group
                    Icon groupIcon = items.get(0).getIcon(); // Use first item's icon
                    TaskbarItem group = new TaskbarItem(entry.getKey(), groupIcon, items);
                    listModel.addElement(group);
                }
            }
        } else {
            // No grouping - add all tools directly
            for (ToolInstance tool : tools.getAllTools()) {
                listModel.addElement(createTaskbarItem(tool));
            }
        }
    }

    /**
     * Create a TaskbarItem from a ToolInstance.
     */
    private TaskbarItem createTaskbarItem(ToolInstance tool) {
        VirtualAppFrame frame = frameCache.get(tool.getId());
        Icon icon = frame != null ? frame.getFrameIcon() : null;

        // Compute docking indicator from tool's docking state
        DockingState dockingState = tool.getDockingState();
        DockingIndicator indicator = DockingIndicator.fromState(
            dockingState.isOriginalPanelPresent(),
            dockingState.hasExternalContent()
        );

        return new TaskbarItem(
            tool.getId(),
            tool.getTitle(),
            tool.getToolType(),
            icon,
            tool.getFrameState(),
            indicator
        );
    }

    /**
     * Update selection to match the selected tool in state.
     */
    private void updateSelection(AppState state) {
        String selectedId = state.getTaskbar().getSelectedToolId();

        if (selectedId == null) {
            if (!taskbarList.isSelectionEmpty()) {
                updatingSelection = true;
                taskbarList.clearSelection();
                updatingSelection = false;
            }
            return;
        }

        // Find and select the item
        for (int i = 0; i < listModel.size(); i++) {
            TaskbarItem item = listModel.get(i);
            if (selectedId.equals(item.getId())) {
                if (taskbarList.getSelectedIndex() != i) {
                    updatingSelection = true;
                    taskbarList.setSelectedIndex(i);
                    updatingSelection = false;
                }
                return;
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() || updatingSelection) {
            return;
        }

        TaskbarItem selected = taskbarList.getSelectedValue();
        if (selected == null) {
            return;
        }

        if (selected.isGroup()) {
            showGroupPopupMenu(selected);
        } else {
            // Activate the tool
            activateTool(selected.getId());
        }
    }

    /**
     * Activate a tool by its ID - restore and bring to front.
     */
    private void activateTool(String toolId) {
        VirtualAppFrame frame = frameCache.get(toolId);
        if (frame == null) {
            return;
        }

        if (!frame.isVisible()) {
            frame.setVisible(true);
        }

        try {
            frame.setIcon(false);
            frame.moveToFront();
            frame.setSelected(true);
        } catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Show a popup menu with all items in a group.
     */
    private void showGroupPopupMenu(TaskbarItem group) {
        JPopupMenu popup = new JPopupMenu(group.getToolType());

        // Add menu item for each tool in the group
        for (final TaskbarItem item : group.getGroupedItems()) {
            JMenuItem menuItem = new JMenuItem(item.getTitle(), item.getIcon());

            // Style minimized items differently
            if (item.getFrameState() == org.jwellman.virtualdesktop.state.model.FrameState.MINIMIZED) {
                menuItem.setFont(menuItem.getFont().deriveFont(java.awt.Font.ITALIC));
            }

            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    activateTool(item.getId());
                }
            });
            popup.add(menuItem);
        }

        // Add separator and "Show All" option
        popup.addSeparator();
        JMenuItem showAllItem = new JMenuItem("Show All (" + group.getGroupSize() + ")");
        showAllItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activateAllInGroup(group);
            }
        });
        popup.add(showAllItem);

        // Show popup at click location or near the selected item
        int selectedIndex = taskbarList.getSelectedIndex();
        if (selectedIndex >= 0 && lastClickX == 0 && lastClickY == 0) {
            // Fallback: show near the cell if no click location recorded
            Rectangle cellBounds = taskbarList.getCellBounds(selectedIndex, selectedIndex);
            if (cellBounds != null) {
                popup.show(taskbarList, cellBounds.x + cellBounds.width, cellBounds.y);
                return;
            }
        }
        popup.show(taskbarList, lastClickX, lastClickY);
    }

    /**
     * Activate all tools in a group - restore and tile them.
     */
    private void activateAllInGroup(TaskbarItem group) {
        for (TaskbarItem item : group.getGroupedItems()) {
            VirtualAppFrame frame = frameCache.get(item.getId());
            if (frame != null) {
                if (!frame.isVisible()) {
                    frame.setVisible(true);
                }
                try {
                    frame.setIcon(false);
                } catch (PropertyVetoException ex) {
                    // Ignore
                }
            }
        }

        // Bring the first one to front and select it
        if (!group.getGroupedItems().isEmpty()) {
            TaskbarItem first = group.getGroupedItems().get(0);
            VirtualAppFrame frame = frameCache.get(first.getId());
            if (frame != null) {
                try {
                    frame.moveToFront();
                    frame.setSelected(true);
                } catch (PropertyVetoException ex) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Unsubscribe from store when controller is no longer needed.
     */
    public void dispose() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    /**
     * Get the managed JList.
     */
    public JList<TaskbarItem> getTaskbarList() {
        return taskbarList;
    }

    /**
     * Toggle taskbar grouping on/off.
     */
    public void toggleGrouping() {
        boolean currentlyEnabled = AppStore.get().getState().getTaskbar().isGroupingEnabled();
        AppStore.get().dispatch(SimpleAction.taskbarGroupingToggled(!currentlyEnabled));
    }

    /**
     * Set taskbar grouping explicitly.
     * @param enabled true to enable grouping, false to disable
     */
    public void setGroupingEnabled(boolean enabled) {
        AppStore.get().dispatch(SimpleAction.taskbarGroupingToggled(enabled));
    }

    /**
     * Check if grouping is currently enabled.
     * @return true if grouping is enabled
     */
    public boolean isGroupingEnabled() {
        return AppStore.get().getState().getTaskbar().isGroupingEnabled();
    }

    // ========== Static convenience methods for BeanShell access ==========

    /**
     * Toggle taskbar grouping (static convenience method).
     * Can be called from BeanShell: TaskbarController.toggleTaskbarGrouping()
     */
    public static void toggleTaskbarGrouping() {
        boolean currentlyEnabled = AppStore.get().getState().getTaskbar().isGroupingEnabled();
        AppStore.get().dispatch(SimpleAction.taskbarGroupingToggled(!currentlyEnabled));
    }

    /**
     * Enable or disable taskbar grouping (static convenience method).
     * Can be called from BeanShell: TaskbarController.setTaskbarGrouping(true)
     */
    public static void setTaskbarGrouping(boolean enabled) {
        AppStore.get().dispatch(SimpleAction.taskbarGroupingToggled(enabled));
    }

}
