package org.jwellman.virtualdesktop.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

/**
 * Desktop shortcut tile — rendering and input only.
 *
 * <p>Does not hold Actions or mutate application state. User gestures are
 * reported through {@link TileListener} to the owning {@link DesktopView}.</p>
 *
 * @author Rick Wellman
 */
public class VShortcut extends JLabel {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(VShortcut.class.getName());

    private static final Border PADDING = BorderFactory.createEmptyBorder(12, 12, 12, 12);

    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 11);

    /** Callback for gesture reporting (set by ClassicDesktopView). */
    public interface TileListener {
        void onSelected(String shortcutId);
        void onActivated(String shortcutId);
        void onMoved(String shortcutId, int x, int y);
        void onContextRequested(String shortcutId, Point screenPoint);
    }

    private String shortcutId;
    private boolean external;
    private TileListener tileListener;
    private boolean selected;
    private JLayeredPane pane;
    private MyMouseMotionListener mml;

    @SuppressWarnings("unused")
    private ComponentUI oldUI;

    protected static MyUI myui = new MyUI(new Color(24, 26, 32, 140));
    protected static MyUI actui = new MyUI(new Color(0, 0, 255, 20));
    protected static MyUI selui = new MyUI(new Color(0, 0, 255, 55));
    protected static MyUI selactui = new MyUI(new Color(0, 0, 255, 90));
    protected static boolean transparentBg = true;

    /**
     * Primary constructor for controller-driven desktops.
     */
    public VShortcut(String shortcutId, String label, Icon icon, boolean external, int x, int y) {
        super();
        this.shortcutId = shortcutId;
        this.external = external;
        this.setText(label);
        this.setFont(LABEL_FONT);
        this.setHorizontalAlignment(JLabel.CENTER);
        this.setOpaque(false);
        this.setVerticalTextPosition(JLabel.BOTTOM);
        this.setHorizontalTextPosition(JLabel.CENTER);

        oldUI = getUI();
        if (transparentBg) {
            setUI(myui);
        }

        this.setLocation(x, y);
        this.setBorder(PADDING);
        this.setIcon(icon);

        this.setSize(this.getPreferredSize());
        final Dimension d = this.getSize();
        this.setSize(IconRegistryLoader.getShortcutWidth(), d.height);
        this.setVisible(true);

        initMouseListeners();
    }

    /**
     * Apply updated view-model data without recreating the component.
     */
    public void applyItem(DesktopShortcutItem item) {
        this.shortcutId = item.getId();
        this.external = item.isExternal();
        this.setText(item.getLabel());
        this.setIcon(item.getIcon());
        if (this.getX() != item.getX() || this.getY() != item.getY()) {
            this.setLocation(item.getX(), item.getY());
        }
        this.setSize(this.getPreferredSize());
        final Dimension d = this.getSize();
        this.setSize(IconRegistryLoader.getShortcutWidth(), d.height);
        repaint();
    }

    public void setTileListener(TileListener listener) {
        this.tileListener = listener;
    }

    public String getShortcutId() {
        return shortcutId;
    }

    public boolean isExternal() {
        return external;
    }

    /**
     * Visual selection driven by DesktopView.setSelectedId — not static globals.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        if (transparentBg) {
            setUI(selected ? selui : myui);
        }
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    /**
     * Optional layered pane for drag elevation (when set by the host).
     */
    public void setLayeredPane(JLayeredPane pane) {
        this.pane = pane;
    }

    private void initMouseListeners() {
        addMouseListener(new MyEnterExitAdapter());
        addMouseListener(new MyMouseAdapter());
    }

    private void fireSelected() {
        if (tileListener != null && shortcutId != null) {
            tileListener.onSelected(shortcutId);
        }
    }

    private void fireActivated() {
        if (tileListener != null && shortcutId != null) {
            tileListener.onActivated(shortcutId);
        }
    }

    private void fireMoved() {
        if (tileListener != null && shortcutId != null) {
            tileListener.onMoved(shortcutId, getX(), getY());
        }
    }

    private void fireContext(MouseEvent ev) {
        if (tileListener != null && shortcutId != null) {
            tileListener.onContextRequested(shortcutId, ev.getLocationOnScreen());
        }
    }

    private class MyEnterExitAdapter extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent ev) {
            if (transparentBg) {
                setUI(selected ? selactui : actui);
            }
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent ev) {
            if (transparentBg) {
                setUI(selected ? selui : myui);
            }
            repaint();
        }
    }

    private class MyMouseAdapter extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent ev) {
            if (ev.getClickCount() == 2 && ev.getButton() == MouseEvent.BUTTON1) {
                log.log(Level.FINER, "Activating shortcut: {0}", shortcutId);
                fireActivated();
            }
        }

        @Override
        public void mousePressed(MouseEvent ev) {
            if (ev.isPopupTrigger()) {
                fireSelected();
                fireContext(ev);
                return;
            }

            if (ev.getButton() != MouseEvent.BUTTON1) {
                return;
            }

            fireSelected();
            createMotionListener(ev);
            VShortcut.this.addMouseMotionListener(mml);
        }

        private void createMotionListener(MouseEvent ev) {
            final int offx = ev.getX();
            final int offy = ev.getY();
            if (mml == null) {
                mml = new MyMouseMotionListener();
            }
            mml.setOffsets(offx, offy);
        }

        @Override
        public void mouseReleased(MouseEvent ev) {
            if (ev.isPopupTrigger()) {
                fireSelected();
                fireContext(ev);
                if (mml != null) {
                    VShortcut.this.removeMouseMotionListener(mml);
                }
                return;
            }

            if (mml != null) {
                VShortcut.this.removeMouseMotionListener(mml);
                if (mml.didDrag()) {
                    fireMoved();
                }
            }

            if (pane != null) {
                pane.setLayer(VShortcut.this, JLayeredPane.DEFAULT_LAYER.intValue(), 0);
            }
        }
    }

    private class MyMouseMotionListener extends MouseMotionAdapter {

        int offx;
        int offy;
        private boolean dragged;

        public void setOffsets(int x, int y) {
            offx = x;
            offy = y;
            dragged = false;
        }

        public boolean didDrag() {
            return dragged;
        }

        @Override
        public void mouseMoved(MouseEvent ev) {
            mouseDragged(ev);
        }

        @Override
        public void mouseDragged(MouseEvent ev) {
            dragged = true;
            Point pt = getLocation();
            Point p = new Point(ev.getX() + pt.x - offx, ev.getY() + pt.y - offy);

            int xoff = p.x % 5;
            int yoff = p.y % 5;
            p = new Point(p.x - xoff + 5, p.y - yoff + 5);

            if (pane != null) {
                pane.setLayer(VShortcut.this, JLayeredPane.DRAG_LAYER.intValue());
            }

            setLocation(p.x, p.y);
        }
    }

    @SuppressWarnings("restriction")
    private static class MyUI extends com.sun.java.swing.plaf.windows.WindowsLabelUI {

        private static final Color MUTED_GOLD = new Color(160, 151, 124);
        private static final Stroke STROKE_1_0 = new BasicStroke(1.5f);

        Color col;

        public MyUI(Color c) {
            col = c;
        }

        @Override
        public void update(Graphics g, JComponent c) {
            if (!transparentBg) {
                super.update(g, c);
            } else {
                final Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final int w = c.getWidth() - 1;
                final int h = c.getHeight() - 1;
                final int arc = 15;

                final Color original = g2.getColor();

                g2.setColor(col);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                g2.setColor(MUTED_GOLD);
                g2.setStroke(STROKE_1_0);
                g2.drawRoundRect(0, 0, w, h, arc, arc);

                g2.setColor(original);

                super.update(g, c);

                if (c instanceof VShortcut && ((VShortcut) c).external) {
                    final JLabel label = (JLabel) c;
                    final Icon icon = label.getIcon();
                    if (icon != null) {
                        final java.awt.Insets ins = c.getInsets();
                        final int iy2 = ins.top + icon.getIconHeight();

                        final int AS = 8;
                        final int AH = 3;

                        final int tipX = w - 6;
                        final int tipY = iy2 - AS + 3;

                        final Stroke savedStroke = g2.getStroke();
                        g2.setStroke(STROKE_1_0);
                        g2.setColor(MUTED_GOLD);

                        g2.drawLine(tipX - AS, tipY + AS, tipX, tipY);
                        g2.drawLine(tipX, tipY, tipX - AH, tipY);
                        g2.drawLine(tipX, tipY, tipX, tipY + AH);

                        g2.setColor(original);
                        g2.setStroke(savedStroke);
                    }
                }
            }
        }
    }

}
