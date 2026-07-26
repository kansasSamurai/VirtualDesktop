package org.jwellman.virtualdesktop.desktop;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.model.AppState;
import org.jwellman.virtualdesktop.state.model.DesktopState;
import org.jwellman.virtualdesktop.state.model.ShortcutInstance;
import org.jwellman.virtualdesktop.state.store.AppStore;
import org.jwellman.virtualdesktop.state.store.StoreSubscriber;
import org.jwellman.virtualdesktop.state.store.Subscription;
import org.jwellman.virtualdesktop.tools.ToolDefinition;
import org.jwellman.virtualdesktop.tools.ToolEnvironment;
import org.jwellman.virtualdesktop.tools.ToolIcons;
import org.jwellman.virtualdesktop.tools.ToolService;
import org.jwellman.virtualdesktop.vapps.ActionFactory;
import org.jwellman.virtualdesktop.vapps.DesktopAction;

/**
 * Bridges Redux {@link DesktopState} to a {@link DesktopView}.
 *
 * <p>Seeds catalog desktop-only shortcuts on first construction when the store
 * slice is empty. Holds only the view interface — a second desktop look ships
 * by swapping the {@link DesktopView} implementation.</p>
 */
public class DesktopController implements StoreSubscriber, DesktopViewListener {

    private final DesktopView view;
    private final Subscription subscription;
    private final ToolService toolService;

    public DesktopController(DesktopView view) {
        this.view = view;
        this.view.setListener(this);
        this.toolService = ToolEnvironment.service();

        seedShortcutsIfEmpty();

        this.subscription = AppStore.get().subscribe(this);
        onStateChanged(AppStore.get().getState());
    }

    private void seedShortcutsIfEmpty() {
        DesktopState desktop = AppStore.get().getState().getDesktop();
        if (desktop.getShortcutCount() > 0) {
            return;
        }

        // Historic VShortcut.init swapped call-site (x,y+=80) into setLocation(y, x),
        // producing a horizontal row at y=10. Preserve that layout.
        int x = -70;
        int y = 10;

        for (DesktopAction action : ActionFactory.getListOfActions()) {
            if (!action.isDesktopOnly()) {
                continue;
            }
            String definitionId = action.getDefinitionId();
            if (definitionId == null || definitionId.isEmpty()) {
                continue;
            }

            String label = definitionId;
            Object name = action.getValue(javax.swing.Action.NAME);
            if (name != null) {
                label = name.toString();
            }

            String iconKey = action.getIconKey();
            if (iconKey == null || iconKey.isEmpty()) {
                ToolDefinition def = ToolEnvironment.catalog().findById(definitionId);
                if (def != null) {
                    iconKey = def.getIconKey();
                }
            }

            x += 80;
            ShortcutInstance shortcut = ShortcutInstance.create(
                action.getShortcutId(),
                label,
                iconKey,
                definitionId,
                x,
                y,
                action.isExternal()
            );
            AppStore.get().dispatch(SimpleAction.shortcutAdded(shortcut));
        }
    }

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

        List<DesktopShortcutItem> items = buildItems(state.getDesktop());
        view.setShortcuts(items);
        view.setSelectedId(state.getDesktop().getSelectedShortcutId());
    }

    private List<DesktopShortcutItem> buildItems(DesktopState desktop) {
        List<DesktopShortcutItem> items = new ArrayList<DesktopShortcutItem>();
        for (ShortcutInstance s : desktop.getAllShortcuts()) {
            Icon icon = ToolIcons.resolveLarge(s.getIconKey());
            items.add(new DesktopShortcutItem(
                s.getId(),
                s.getLabel(),
                icon,
                s.getX(),
                s.getY(),
                s.isExternal(),
                s.getDefinitionId()
            ));
        }
        return items;
    }

    @Override
    public void onShortcutSelected(String shortcutId) {
        AppStore.get().dispatch(SimpleAction.shortcutSelected(shortcutId));
    }

    @Override
    public void onShortcutActivated(String shortcutId) {
        AppStore.get().dispatch(SimpleAction.shortcutInvoked(shortcutId));
        ShortcutInstance shortcut = AppStore.get().getState().getDesktop().getShortcut(shortcutId);
        if (shortcut != null && shortcut.getDefinitionId() != null) {
            toolService.open(shortcut.getDefinitionId());
        }
    }

    @Override
    public void onShortcutMoved(String shortcutId, int x, int y) {
        AppStore.get().dispatch(SimpleAction.shortcutMoved(shortcutId, x, y));
    }

    @Override
    public void onShortcutContextRequested(String shortcutId, Point screenPoint) {
        // Reserved — no popup yet; still select so state stays consistent
        AppStore.get().dispatch(SimpleAction.shortcutSelected(shortcutId));
    }

    @Override
    public void onBackgroundClicked() {
        AppStore.get().dispatch(SimpleAction.shortcutDeselected());
    }

    public void dispose() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    public DesktopView getView() {
        return view;
    }

}
