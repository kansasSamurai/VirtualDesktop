package org.jwellman.virtualdesktop.state.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable Redux slice for the desktop surface (shortcuts + selection).
 */
public final class DesktopState {

    private final Map<String, ShortcutInstance> shortcutsById;
    private final String selectedShortcutId;

    private DesktopState(Map<String, ShortcutInstance> shortcutsById, String selectedShortcutId) {
        this.shortcutsById = shortcutsById;
        this.selectedShortcutId = selectedShortcutId;
    }

    public static DesktopState empty() {
        return new DesktopState(
            Collections.<String, ShortcutInstance>emptyMap(),
            null
        );
    }

    public Map<String, ShortcutInstance> getShortcutsById() {
        return shortcutsById;
    }

    public Collection<ShortcutInstance> getAllShortcuts() {
        return shortcutsById.values();
    }

    public ShortcutInstance getShortcut(String id) {
        return shortcutsById.get(id);
    }

    public String getSelectedShortcutId() {
        return selectedShortcutId;
    }

    public int getShortcutCount() {
        return shortcutsById.size();
    }

    public DesktopState withShortcutAdded(ShortcutInstance shortcut) {
        Map<String, ShortcutInstance> next = new LinkedHashMap<String, ShortcutInstance>(shortcutsById);
        next.put(shortcut.getId(), shortcut);
        return new DesktopState(Collections.unmodifiableMap(next), selectedShortcutId);
    }

    public DesktopState withShortcutUpdated(ShortcutInstance shortcut) {
        if (!shortcutsById.containsKey(shortcut.getId())) {
            return this;
        }
        Map<String, ShortcutInstance> next = new LinkedHashMap<String, ShortcutInstance>(shortcutsById);
        next.put(shortcut.getId(), shortcut);
        return new DesktopState(Collections.unmodifiableMap(next), selectedShortcutId);
    }

    public DesktopState withShortcutRemoved(String id) {
        if (!shortcutsById.containsKey(id)) {
            return this;
        }
        Map<String, ShortcutInstance> next = new LinkedHashMap<String, ShortcutInstance>(shortcutsById);
        next.remove(id);
        String selected = id.equals(selectedShortcutId) ? null : selectedShortcutId;
        return new DesktopState(Collections.unmodifiableMap(next), selected);
    }

    public DesktopState withSelectedShortcutId(String id) {
        return new DesktopState(shortcutsById, id);
    }

    @Override
    public String toString() {
        return "DesktopState{shortcuts=" + shortcutsById.size() + ", selected=" + selectedShortcutId + "}";
    }

}
