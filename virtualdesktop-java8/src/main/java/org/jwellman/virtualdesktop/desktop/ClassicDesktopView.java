package org.jwellman.virtualdesktop.desktop;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;

import org.jwellman.virtualdesktop.vswing.VDesktopPane;

/**
 * Classic free-form desktop: {@link VDesktopPane} + {@link VShortcut} tiles.
 *
 * <p>Manages only shortcut components — never removes tool frames that share
 * the same desktop pane.</p>
 */
public class ClassicDesktopView implements DesktopView {

    private final VDesktopPane desktop;
    private final Map<String, VShortcut> tiles = new HashMap<String, VShortcut>();
    private DesktopViewListener listener;

    public ClassicDesktopView() {
        this(new VDesktopPane());
    }

    public ClassicDesktopView(VDesktopPane desktop) {
        this.desktop = desktop;
        this.desktop.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && listener != null) {
                    // Only clear when the click landed on the desktop itself
                    Component deepest = desktop.findComponentAt(e.getPoint());
                    if (deepest == null || deepest == desktop) {
                        listener.onBackgroundClicked();
                    }
                }
            }
        });
    }

    /**
     * @return the underlying desktop pane (for DesktopManager / scroll host)
     */
    public JDesktopPane getDesktopPane() {
        return desktop;
    }

    @Override
    public JComponent getComponent() {
        return desktop;
    }

    @Override
    public void setShortcuts(List<DesktopShortcutItem> shortcuts) {
        Set<String> keep = new HashSet<String>();
        for (DesktopShortcutItem item : shortcuts) {
            keep.add(item.getId());
            VShortcut tile = tiles.get(item.getId());
            if (tile == null) {
                tile = new VShortcut(
                    item.getId(),
                    item.getLabel(),
                    item.getIcon(),
                    item.isExternal(),
                    item.getX(),
                    item.getY()
                );
                tile.setLayeredPane(desktop);
                tile.setTileListener(createTileForwarder());
                tiles.put(item.getId(), tile);
                desktop.add(tile);
            } else {
                tile.applyItem(item);
            }
        }

        Set<String> toRemove = new HashSet<String>(tiles.keySet());
        toRemove.removeAll(keep);
        for (String id : toRemove) {
            VShortcut tile = tiles.remove(id);
            if (tile != null) {
                desktop.remove(tile);
            }
        }
        desktop.revalidate();
        desktop.repaint();
    }

    @Override
    public void setSelectedId(String shortcutId) {
        for (Map.Entry<String, VShortcut> entry : tiles.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(shortcutId));
        }
    }

    @Override
    public void setListener(DesktopViewListener listener) {
        this.listener = listener;
    }

    private VShortcut.TileListener createTileForwarder() {
        return new VShortcut.TileListener() {
            @Override
            public void onSelected(String shortcutId) {
                if (listener != null) {
                    listener.onShortcutSelected(shortcutId);
                }
            }

            @Override
            public void onActivated(String shortcutId) {
                if (listener != null) {
                    listener.onShortcutActivated(shortcutId);
                }
            }

            @Override
            public void onMoved(String shortcutId, int x, int y) {
                if (listener != null) {
                    listener.onShortcutMoved(shortcutId, x, y);
                }
            }

            @Override
            public void onContextRequested(String shortcutId, Point screenPoint) {
                if (listener != null) {
                    listener.onShortcutContextRequested(shortcutId, screenPoint);
                }
            }
        };
    }

}
