package org.jwellman.swing.grid;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Tree zone strip — a 36px column that renders expand/collapse toggles and depth
 * indentation for hierarchical rows. Lives to the right of the checkbox strip and
 * to the left of the data columns. Off by default; activated via
 * {@code SmartGrid.setTreeZoneVisible(true)}.
 *
 * <p>Moving tree chrome here (instead of baking it into column 0 of StandardRowPanel)
 * means data columns always start at the same x coordinate regardless of row depth.
 */
class TreeZoneStrip implements Strip {

    static final int WIDTH     = 36;
    static final int INDENT_PX = 12;

    private final Runnable expandCollapseAction;
    private JPanel[] slots;
    private boolean  visible = false;

    TreeZoneStrip(Runnable expandCollapseAction) {
        this.expandCollapseAction = expandCollapseAction;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void reallocate(int slotCount, JPanel canvas) {
        if (slots != null) {
            for (JPanel slot : slots) {
                canvas.remove(slot);
            }
        }
        if (slotCount == 0) {
            slots = null;
            return;
        }
        slots = new JPanel[slotCount];
        for (int i = 0; i < slotCount; i++) {
            slots[i] = new JPanel(null);
            slots[i].setOpaque(false);
            canvas.add(slots[i]);
        }
    }

    @Override
    public void bind(int slotIdx, int xOffset, int y, int rowHeight, int modelIdx, GridRow row) {
        if (slots == null || slotIdx >= slots.length) {
            return;
        }
        JPanel slot = slots[slotIdx];
        slot.removeAll();
        slot.setBounds(xOffset, y, WIDTH, rowHeight);

        int indent = row.getDepth() * INDENT_PX;

        if (row.isHasChildren()) {
            JLabel toggle = new JLabel(row.isExpanded() ? "▼" : "▶");
            toggle.setHorizontalAlignment(SwingConstants.CENTER);
            toggle.setBounds(indent, 0, WIDTH - indent, rowHeight);
            final GridRow capturedRow = row;
            final Runnable action     = expandCollapseAction;
            toggle.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    capturedRow.setExpanded(!capturedRow.isExpanded());
                    action.run();
                }
            });
            slot.add(toggle);
        } else if (indent > 0) {
            JLabel spacer = new JLabel();
            spacer.setBounds(0, 0, indent, rowHeight);
            slot.add(spacer);
        }

        slot.setVisible(true);
        slot.revalidate();
        slot.repaint();
    }

    @Override
    public void hide(int slotIdx) {
        if (slots != null && slotIdx < slots.length) {
            slots[slotIdx].setVisible(false);
        }
    }

    @Override
    public JComponent headerCell(int height) {
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        return empty;
    }

    @Override
    public JComponent filterSpacer(int height) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        return spacer;
    }

    @Override
    public JComponent footerSpacer(int height) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        return spacer;
    }
}
