package org.jwellman.virtualdesktop.taskbar;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;
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

    public TaskbarController(JList<TaskbarItem> taskbarList) {
        this.taskbarList = taskbarList;
        this.listModel = new DefaultListModel<>();
        this.taskbarList.setModel(listModel);
        this.taskbarList.setCellRenderer(new TaskbarItemRenderer());
        this.taskbarList.addListSelectionListener(this);

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
            // TODO: Show popup menu with grouped items
            System.out.println("Group selected: " + selected.getTitle());
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
