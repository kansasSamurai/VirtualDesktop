package org.jwellman.virtualdesktop.taskbar;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.model.AppState;
import org.jwellman.virtualdesktop.state.model.DockingState;
import org.jwellman.virtualdesktop.state.model.FrameState;
import org.jwellman.virtualdesktop.state.model.ToolInstance;
import org.jwellman.virtualdesktop.state.model.ToolsState;
import org.jwellman.virtualdesktop.state.store.AppStore;
import org.jwellman.virtualdesktop.state.store.StoreSubscriber;
import org.jwellman.virtualdesktop.state.store.Subscription;
import org.jwellman.virtualdesktop.tools.ToolEnvironment;
import org.jwellman.virtualdesktop.tools.ToolService;

/**
 * Bridges Redux state to a WindowListView implementation.
 *
 * Subscribes to the store and pushes updated item lists to the view on every
 * state change. Implements WindowListViewListener to handle user interactions
 * reported by the view. The controller never references concrete view classes;
 * swapping the view (JList, SmartGrid, etc.) requires no changes here.
 *
 * <p>Lifecycle operations go through {@link ToolService} — not DesktopManager —
 * so this controller stays on the application interface.</p>
 *
 * @author rwellman
 */
public class WindowListController implements StoreSubscriber, WindowListViewListener {

    private final WindowListView view;
    private final Subscription subscription;
    private final ToolService toolService;

    // Last item list sent to the view; retained so context menu handlers can
    // look up group children by id without re-querying Redux state.
    private List<WindowListItem> currentItems = new ArrayList<>();

    public WindowListController(WindowListView view) {
        this.view = view;
        this.view.setListener(this);
        this.toolService = ToolEnvironment.service();

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
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    onStateChanged(state);
                }
            });
            return;
        }

        currentItems = buildItems(state);
        view.setItems(currentItems);
        view.setSelectedId(state.getWindowList().getSelectedToolId());
    }

    // -------------------------------------------------------------------------
    // WindowListViewListener
    // -------------------------------------------------------------------------

    @Override
    public void onItemSelected(String toolId, boolean isGroup) {
        if (!isGroup) {
            toolService.activate(toolId);
        }
    }

    @Override
    public void onContextRequested(String id, boolean isGroup, Point screenPoint) {
        if (isGroup) {
            WindowListItem group = findItemById(id);
            if (group != null) {
                showGroupPopupMenu(group, screenPoint);
            }
        }
    }

    @Override
    public void onItemCloseRequested(String toolId) {
        toolService.close(toolId);
    }

    @Override
    public void onItemActivated(String id, boolean isGroup) {
        if (isGroup) {
            WindowListItem group = findItemById(id);
            if (group != null) {
                activateAllInGroup(group);
            }
        } else {
            toolService.activate(id);
        }
    }

    // -------------------------------------------------------------------------
    // State building
    // -------------------------------------------------------------------------

    private List<WindowListItem> buildItems(AppState state) {
        ToolsState tools = state.getTools();
        boolean groupingEnabled = state.getWindowList().isGroupingEnabled();
        List<WindowListItem> items = new ArrayList<>();

        if (groupingEnabled) {
            Map<String, List<WindowListItem>> groups = new HashMap<>();

            for (ToolInstance tool : tools.getAllTools()) {
                String type = tool.getToolType() != null ? tool.getToolType() : "Unknown";
                WindowListItem item = createWindowListItem(tool);

                if (!groups.containsKey(type)) {
                    groups.put(type, new ArrayList<WindowListItem>());
                }
                groups.get(type).add(item);
            }

            for (Map.Entry<String, List<WindowListItem>> entry : groups.entrySet()) {
                List<WindowListItem> groupMembers = entry.getValue();
                if (groupMembers.size() == 1) {
                    items.add(groupMembers.get(0));
                } else {
                    Icon groupIcon = groupMembers.get(0).getIcon();
                    items.add(new WindowListItem(entry.getKey(), groupIcon, groupMembers));
                }
            }
        } else {
            for (ToolInstance tool : tools.getAllTools()) {
                items.add(createWindowListItem(tool));
            }
        }

        return items;
    }

    private WindowListItem createWindowListItem(ToolInstance tool) {
        Icon icon = toolService.getToolIcon(tool.getId());

        DockingState dockingState = tool.getDockingState();
        DockingIndicator indicator = DockingIndicator.fromState(
                dockingState.isOriginalPanelPresent(),
                dockingState.hasExternalContent());

        return new WindowListItem(
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

    private void showGroupPopupMenu(WindowListItem group, Point screenPoint) {
        JPopupMenu popup = new JPopupMenu(group.getToolType());

        for (final WindowListItem item : group.getGroupedItems()) {
            JMenuItem menuItem = new JMenuItem(item.getTitle(), item.getIcon());

            if (item.getFrameState() == FrameState.MINIMIZED) {
                menuItem.setFont(menuItem.getFont().deriveFont(java.awt.Font.ITALIC));
            }

            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toolService.activate(item.getId());
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

    private void activateAllInGroup(WindowListItem group) {
        List<WindowListItem> members = group.getGroupedItems();
        for (WindowListItem item : members) {
            toolService.restore(item.getId());
        }
        if (!members.isEmpty()) {
            toolService.activate(members.get(0).getId());
        }
    }

    private WindowListItem findItemById(String id) {
        for (WindowListItem item : currentItems) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Applies a theme to the view. Delegates directly to the view; no Redux
     * dispatch needed since theme is managed outside the store.
     */
    public void applyTheme(WindowListTheme theme) {
        view.applyTheme(theme);
    }

    public void dispose() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    public WindowListView getView() {
        return view;
    }

    public void toggleGrouping() {
        boolean currentlyEnabled = AppStore.get().getState().getWindowList().isGroupingEnabled();
        AppStore.get().dispatch(SimpleAction.windowListGroupingToggled(!currentlyEnabled));
    }

    public void setGroupingEnabled(boolean enabled) {
        AppStore.get().dispatch(SimpleAction.windowListGroupingToggled(enabled));
    }

    public boolean isGroupingEnabled() {
        return AppStore.get().getState().getWindowList().isGroupingEnabled();
    }

    // -------------------------------------------------------------------------
    // Static convenience methods for BeanShell access
    // -------------------------------------------------------------------------

    public static void toggleWindowListGrouping() {
        boolean currentlyEnabled = AppStore.get().getState().getWindowList().isGroupingEnabled();
        AppStore.get().dispatch(SimpleAction.windowListGroupingToggled(!currentlyEnabled));
    }

    public static void setWindowListGrouping(boolean enabled) {
        AppStore.get().dispatch(SimpleAction.windowListGroupingToggled(enabled));
    }

}
