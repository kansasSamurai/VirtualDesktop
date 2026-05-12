package org.jwellman.swing.grid;

import java.awt.event.ActionListener;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Checkbox selection strip — the leftmost 28px column of per-row checkboxes plus a
 * "select all" checkbox in the header. Encapsulates all checkbox state that previously
 * lived as ad-hoc fields inside SmartGrid.
 */
class CheckboxStrip implements Strip {

    static final int WIDTH = 28;

    private final DefaultListSelectionModel selectionModel;
    private final GridModel                 model;
    private final Runnable                  selectAll;
    private final Runnable                  clearSelection;

    private JCheckBox[] slots;
    private JCheckBox   headerSelectAll;
    private boolean     visible        = true;
    private boolean     updatingHeader = false;

    CheckboxStrip(DefaultListSelectionModel selectionModel,
                  GridModel model,
                  Runnable selectAll,
                  Runnable clearSelection) {
        this.selectionModel = selectionModel;
        this.model          = model;
        this.selectAll      = selectAll;
        this.clearSelection = clearSelection;

        headerSelectAll = new JCheckBox();
        headerSelectAll.setOpaque(false);
        headerSelectAll.setHorizontalAlignment(SwingConstants.CENTER);
        headerSelectAll.addActionListener(e -> {
            if (updatingHeader) {
                return;
            }
            if (headerSelectAll.isSelected()) {
                selectAll.run();
            } else {
                clearSelection.run();
            }
        });

        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateHeaderCheckbox();
            }
        });
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
            for (JCheckBox cb : slots) {
                canvas.remove(cb);
            }
        }
        if (slotCount == 0) {
            slots = null;
            return;
        }
        slots = new JCheckBox[slotCount];
        for (int i = 0; i < slotCount; i++) {
            slots[i] = new JCheckBox();
            slots[i].setOpaque(false);
            slots[i].setHorizontalAlignment(SwingConstants.CENTER);
            canvas.add(slots[i]);
        }
    }

    @Override
    public void bind(int slotIdx, int xOffset, int y, int rowHeight, int modelIdx, GridRow row) {
        if (slots == null || slotIdx >= slots.length) {
            return;
        }
        JCheckBox cb = slots[slotIdx];
        for (ActionListener al : cb.getActionListeners()) {
            cb.removeActionListener(al);
        }
        cb.setBounds(xOffset, y, WIDTH, rowHeight);
        cb.setSelected(selectionModel.isSelectedIndex(modelIdx));
        final int capturedIdx = modelIdx;
        cb.addActionListener(ae -> {
            if (cb.isSelected()) {
                selectionModel.addSelectionInterval(capturedIdx, capturedIdx);
            } else {
                selectionModel.removeSelectionInterval(capturedIdx, capturedIdx);
            }
        });
        cb.setVisible(true);
    }

    @Override
    public void hide(int slotIdx) {
        if (slots != null && slotIdx < slots.length) {
            slots[slotIdx].setVisible(false);
        }
    }

    @Override
    public JComponent headerCell(int height) {
        return headerSelectAll;
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

    private void updateHeaderCheckbox() {
        if (headerSelectAll == null) {
            return;
        }
        int total = model.getRowCount();
        int count = 0;
        for (int i = 0; i < total; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                count++;
            }
        }
        updatingHeader = true;
        headerSelectAll.setSelected(total > 0 && count == total);
        updatingHeader = false;
    }
}
