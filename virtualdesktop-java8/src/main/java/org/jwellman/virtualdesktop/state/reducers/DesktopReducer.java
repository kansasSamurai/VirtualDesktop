package org.jwellman.virtualdesktop.state.reducers;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.actions.ActionTypes;
import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.model.DesktopState;
import org.jwellman.virtualdesktop.state.model.ShortcutInstance;

/**
 * Reducer for desktop shortcut state.
 */
public class DesktopReducer {

    public DesktopState reduce(DesktopState state, Action action) {
        switch (action.getType()) {
            case ActionTypes.SHORTCUT_ADDED:
                return handleAdded(state, action);
            case ActionTypes.SHORTCUT_REMOVED:
                return handleRemoved(state, action);
            case ActionTypes.SHORTCUT_SELECTED:
                return handleSelected(state, action);
            case ActionTypes.SHORTCUT_DESELECTED:
                return state.withSelectedShortcutId(null);
            case ActionTypes.SHORTCUT_MOVED:
                return handleMoved(state, action);
            case ActionTypes.SHORTCUT_INVOKED:
                // Lifecycle side effect is handled by DesktopController; no state change.
                return state;
            default:
                return state;
        }
    }

    private DesktopState handleAdded(DesktopState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof ShortcutInstance) {
            return state.withShortcutAdded((ShortcutInstance) payload);
        }
        return state;
    }

    private DesktopState handleRemoved(DesktopState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            return state.withShortcutRemoved((String) payload);
        }
        return state;
    }

    private DesktopState handleSelected(DesktopState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            return state.withSelectedShortcutId((String) payload);
        }
        return state;
    }

    private DesktopState handleMoved(DesktopState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof SimpleAction.ShortcutMovedPayload) {
            SimpleAction.ShortcutMovedPayload p = (SimpleAction.ShortcutMovedPayload) payload;
            ShortcutInstance existing = state.getShortcut(p.shortcutId);
            if (existing != null) {
                return state.withShortcutUpdated(existing.withPosition(p.x, p.y));
            }
        }
        return state;
    }

}
