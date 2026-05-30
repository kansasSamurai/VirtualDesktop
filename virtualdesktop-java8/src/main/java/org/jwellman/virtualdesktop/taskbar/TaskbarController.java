package org.jwellman.virtualdesktop.taskbar;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.jwellman.virtualdesktop.DesktopManager;
import org.jwellman.virtualdesktop.VirtualAppFrame;
import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.model.AppState;
import org.jwellman.virtualdesktop.state.model.DockingState;
import org.jwellman.virtualdesktop.state.model.FrameState;
import org.jwellman.virtualdesktop.state.model.ToolInstance;
import org.jwellman.virtualdesktop.state.model.ToolsState;
import org.jwellman.virtualdesktop.state.store.AppStore;
import org.jwellman.virtualdesktop.state.store.StoreSubscriber;
import org.jwellman.virtualdesktop.state.store.Subscription;

/**
 * Bridges Redux state to a TaskbarView implementation.
 *
 * Subscribes to the store and pushes updated item lists to the view on every
 * state change. Implements TaskbarViewListener to handle user interactions
 * reported by the view. The controller never references concrete view classes;
 * swapping the view (JList, SmartGrid, etc.) requires no changes here.
 *
 * @author rwellman
 */
public class TaskbarController implements StoreSubscriber, TaskbarViewListener {

    private final TaskbarView view;
    private final Subscription subscription;

    // Cache of toolId -> VirtualAppFrame for icon lookup and frame activation
    private final Map<String, VirtualAppFrame> frameCache = new HashMap<>();

    // Last item list sent to the view; retained so context menu handlers can
    // look up group children by id without re-querying Redux state.
    private List<TaskbarItem> currentItems = new ArrayList<>();

    public TaskbarController(TaskbarView view) {
        this.view = view;
        this.view.setListener(this);

        this.subscription = AppStore.get().subscribe(this);

        // Initial render
        onStateChanged(AppStore.get().getState());
    }

    // -------------------------------------------------------------------------
    // StoreSubscriber
    // -------------------------------------------------------------------------

    @Override
    public void onStateChanged(AppState state) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onStateChanged(state));
            return;
        }

        updateFrameCache();
        currentItems = buildItems(state);
        view.setItems(currentItems);
        view.setSelectedId(state.getTaskbar().getSelectedToolId());
    }

    // -------------------------------------------------------------------------
    // TaskbarViewListener
    // -------------------------------------------------------------------------

    @Override
    public void onItemSelected(String toolId, boolean isGroup) {
        if (!isGroup) {
            activateTool(toolId);
        }
    }

    @Override
    public void onContextRequested(String id, boolean isGroup, Point screenPoint) {
        if (isGroup) {
            TaskbarItem group = findItemById(id);
            if (group != null) {
                showGroupPopupMenu(group, screenPoint);
            }
        }
    }

    @Override
    public void onItemActivated(String id, boolean isGroup) {
        if (isGroup) {
            TaskbarItem group = findItemById(id);
            if (group != null) {
                activateAllInGroup(group);
            }
        } else {
            activateTool(id);
        }
    }

    // -------------------------------------------------------------------------
    // State building
    // -------------------------------------------------------------------------

    private void updateFrameCache() {
        frameCache.clear();
        for (VirtualAppFrame frame : DesktopManager.get().getFrames()) {
            frameCache.put(frame.getToolId(), frame);
        }
    }

    private List<TaskbarItem> buildItems(AppState state) {
        ToolsState tools = state.getTools();
        boolean groupingEnabled = state.getTaskbar().isGroupingEnabled();
        List<TaskbarItem> items = new ArrayList<>();

        if (groupingEnabled) {
            Map<String, List<TaskbarItem>> groups = new HashMap<>();

            for (ToolInstance tool : tools.getAllTools()) {
                String type = tool.getToolType() != null ? tool.getToolType() : "Unknown";
                TaskbarItem item = createTaskbarItem(tool);

                if (!groups.containsKey(type)) {
                    groups.put(type, new ArrayList<TaskbarItem>());
                }
                groups.get(type).add(item);
            }

            for (Map.Entry<String, List<TaskbarItem>> entry : groups.entrySet()) {
                List<TaskbarItem> groupMembers = entry.getValue();
                if (groupMembers.size() == 1) {
                    items.add(groupMembers.get(0));
                } else {
                    Icon groupIcon = groupMembers.get(0).getIcon();
                    items.add(new TaskbarItem(entry.getKey(), groupIcon, groupMembers));
                }
            }
        } else {
            for (ToolInstance tool : tools.getAllTools()) {
                items.add(createTaskbarItem(tool));
            }
        }

        return items;
    }

    private TaskbarItem createTaskbarItem(ToolInstance tool) {
        VirtualAppFrame frame = frameCache.get(tool.getId());
        Icon icon = frame != null ? frame.getFrameIcon() : null;

        DockingState dockingState = tool.getDockingState();
        DockingIndicator indicator = DockingIndicator.fromState(
                dockingState.isOriginalPanelPresent(),
                dockingState.hasExternalContent());

        return new TaskbarItem(
                tool.getId(),
                tool.getTitle(),
                tool.getToolType(),
                icon,
                tool.getFrameState(),
                indicator);
    }

    // -------------------------------------------------------------------------
    // Action helpers
    // -------------------------------------------------------------------------

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

    private void showGroupPopupMenu(TaskbarItem group, Point screenPoint) {
        JPopupMenu popup = new JPopupMenu(group.getToolType());

        for (final TaskbarItem item : group.getGroupedItems()) {
            JMenuItem menuItem = new JMenuItem(item.getTitle(), item.getIcon());

            if (item.getFrameState() == FrameState.MINIMIZED) {
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

        popup.addSeparator();
        JMenuItem showAllItem = new JMenuItem("Show All (" + group.getGroupSize() + ")");
        showAllItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activateAllInGroup(group);
            }
        });
        popup.add(showAllItem);

        JComponent comp = view.getComponent();
        Point pt = new Point(screenPoint);
        SwingUtilities.convertPointFromScreen(pt, comp);
        popup.show(comp, pt.x, pt.y);
    }

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

    private TaskbarItem findItemById(String id) {
        for (TaskbarItem item : currentItems) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void dispose() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    public TaskbarView getView() {
        return view;
    }

    public void toggleGrouping() {
        boolean currentlyEnabled = AppStore.get().getState().getTaskbar().isGroupingEnabled();
        AppStore.get().dispatch(SimpleAction.taskbarGroupingToggled(!currentlyEnabled));
    }

    public void setGroupingEnabled(boolean enabled) {
        AppStore.get().dispatch(SimpleAction.taskbarGroupingToggled(enabled));
    }

    public boolean isGroupingEnabled() {
        return AppStore.get().getState().getTaskbar().isGroupingEnabled();
    }

    // -------------------------------------------------------------------------
    // Static convenience methods for BeanShell access
    // -------------------------------------------------------------------------

    public static void toggleTaskbarGrouping() {
        boolean currentlyEnabled = AppStore.get().getState().getTaskbar().isGroupingEnabled();
        AppStore.get().dispatch(SimpleAction.taskbarGroupingToggled(!currentlyEnabled));
    }

    public static void setTaskbarGrouping(boolean enabled) {
        AppStore.get().dispatch(SimpleAction.taskbarGroupingToggled(enabled));
    }

}
