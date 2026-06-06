package org.jwellman.virtualdesktop.taskbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.Recyclable;
import org.jwellman.swing.grid.Selectable;
import org.jwellman.virtualdesktop.state.model.FrameState;

/**
 * SmartGrid row panel for a single active-window entry.
 *
 * Three-column layout: [Icon 16px] → [Title, expanding] → [Close × 16px]
 * Row height target: 32–36px (enforced via preferred size; SmartGrid's rowHeight
 * setting drives the actual height).
 *
 * Visual states:
 *   Active/selected — 2px Muted Gold indicator bar on left edge + translucent
 *                     gold background tint
 *   Minimized       — title text rendered in dimmed foreground color
 *   Progress        — background fills left-to-right as executionProgress 0→1
 *
 * All colors come from a shared WindowListTheme[] reference so that applyTheme()
 * on the view takes effect on the next bind() without recreating pool instances.
 */
@SuppressWarnings("serial")
public class WindowListRowComponent extends JPanel implements Recyclable, Selectable {

    private static final int VERT_INSET = 2;
    private static final int LEFT_INSET = 10; // 2px indicator + 8px gap
    private static final int RIGHT_INSET = 8;
    private static final int ROW_HEIGHT = 34;
    private static final int INDICATOR_WIDTH = 2;

    private final JLabel iconLabel;
    private final JLabel nameLabel;
    private final JButton closeButton;
    @SuppressWarnings("unused")
    private final Consumer<String> onClose;
    private final ListSelectionModel selectionModel;
    private final WindowListTheme[] themeHolder;

    private boolean isSelected = false;
    private float executionProgress = 0.0f;
    private MouseAdapter rowListener = null;
    private String currentToolId = null;

    public interface Resources {
        Border rowBorder();
        Dimension preferredSize();
        Dimension iconPreferredSize();
        Dimension closeButtonPreferredSize();
    }

    public static class Customizer implements Resources {

        private Border rowBorder = BorderFactory.createEmptyBorder(VERT_INSET, LEFT_INSET, VERT_INSET, RIGHT_INSET);
        private Dimension preferredSize = new Dimension(180, ROW_HEIGHT);
        private Dimension iconPreferredSize = new Dimension(20, 16);
        private Dimension closeButtonPreferredSize = new Dimension(20, 20);

        @Override
        public Dimension preferredSize() { return preferredSize; }

        @Override
        public Dimension iconPreferredSize() { return iconPreferredSize; }

        @Override
        public Border rowBorder() { return rowBorder; }

        @Override
        public Dimension closeButtonPreferredSize() { return closeButtonPreferredSize; }

    }

    public static Customizer CUSTOMIZER = new Customizer();

    public WindowListRowComponent(Consumer<String> onClose,
                                  ListSelectionModel selectionModel,
                                  int[] columnWidths,
                                  WindowListTheme[] themeHolder) {
        this.onClose        = onClose;
        this.selectionModel = selectionModel;
        this.themeHolder    = themeHolder;

        setOpaque(false);
        setLayout(new BorderLayout(8, 0));
        setBorder(CUSTOMIZER.rowBorder());
        setPreferredSize(CUSTOMIZER.preferredSize());

        iconLabel = new JLabel();
        iconLabel.setPreferredSize(CUSTOMIZER.iconPreferredSize());

        nameLabel = new JLabel();
        nameLabel.setForeground(themeHolder[0].normalForeground);

        closeButton = new JButton("×");
        closeButton.setFont(closeButton.getFont().deriveFont(Font.BOLD, 12f));
        closeButton.setFocusable(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setForeground(themeHolder[0].closeButtonDefault);
        closeButton.setPreferredSize(CUSTOMIZER.closeButtonPreferredSize());

        // 1. Store the colors directly inside the component's dictionary
        closeButton.putClientProperty(HoverColorListener.KEY_DEFAULT_COLOR, themeHolder[0].closeButtonDefault);
        closeButton.putClientProperty(HoverColorListener.KEY_HOVER_COLOR, themeHolder[0].closeButtonHover);

        // 2. Wire it straight to the immutable global Singleton
        closeButton.addMouseListener(HoverColorListener.INSTANCE);

        // ActionListener set once; reads currentToolId at click time.
        closeButton.addActionListener(e -> {
            if (currentToolId != null) {
                onClose.accept(currentToolId);
            }
        });

        add(iconLabel, BorderLayout.WEST);
        add(nameLabel, BorderLayout.CENTER);
        add(closeButton, BorderLayout.EAST);
    }

    // -------------------------------------------------------------------------
    // Recyclable
    // -------------------------------------------------------------------------

    @Override
    public void prepareForReuse() {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }
        iconLabel.setIcon(null);
        nameLabel.setText("");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));
        nameLabel.setForeground(themeHolder[0].normalForeground);
        closeButton.setForeground(themeHolder[0].closeButtonDefault);
        isSelected = false;
        executionProgress = 0.0f;
        currentToolId = null;
    }

    @Override
    public void bind(GridRow row, int rowIndex) {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }

        WindowListItem item = (WindowListItem) row.get("item");
        currentToolId = item.getId();

        iconLabel.setIcon(item.getIcon());

        boolean minimized = item.getFrameState() == FrameState.MINIMIZED;
        nameLabel.setText(item.getTitle());
        nameLabel.setForeground(minimized
            ? themeHolder[0].minimizedForeground
            : themeHolder[0].normalForeground);
        nameLabel.setFont(nameLabel.getFont().deriveFont(
            minimized ? Font.ITALIC : Font.PLAIN));

        closeButton.setForeground(themeHolder[0].closeButtonDefault);

        final int capturedIdx = rowIndex;
        final ListSelectionModel sm = selectionModel;
        rowListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sm.setSelectionInterval(capturedIdx, capturedIdx);
            }
        };
        addMouseListener(rowListener);
    }

    // -------------------------------------------------------------------------
    // Selectable
    // -------------------------------------------------------------------------

    @Override
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        repaint();
    }

    // -------------------------------------------------------------------------
    // Progress API
    // -------------------------------------------------------------------------

    public void setExecutionProgress(float progress) {
        this.executionProgress = Math.max(0.0f, Math.min(1.0f, progress));
        repaint();
    }

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        if (executionProgress > 0.0f) {
            g2.setColor(themeHolder[0].progressTint);
            g2.fillRect(0, 0, (int) (getWidth() * executionProgress), getHeight());
        }

        if (isSelected) {
            g2.setColor(themeHolder[0].activeBackground);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(themeHolder[0].activeIndicator);
            g2.fillRect(0, 0, INDICATOR_WIDTH, getHeight());
        }

        g2.dispose();
    }

    // The mouse adapter can be a global/singleton object as follows...
    private static class HoverColorListener extends MouseAdapter {

        // 1. A single, globally accessible instance
        public static final HoverColorListener INSTANCE = new HoverColorListener();

        // String keys for Swing's internal Client Property map
        public static final String KEY_HOVER_COLOR = "HoverColor.hover";
        public static final String KEY_DEFAULT_COLOR = "HoverColor.default";

        private HoverColorListener() {} // Prevent direct instantiation

        @Override
        public void mouseEntered(MouseEvent e) {
            if (e.getSource() instanceof JComponent) {
                JComponent comp = (JComponent) e.getSource();
                Color hoverColor = (Color) comp.getClientProperty(KEY_HOVER_COLOR);
                if (hoverColor != null) {
                    comp.setForeground(hoverColor);
                }
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (e.getSource() instanceof JComponent) {
                JComponent comp = (JComponent) e.getSource();
                Color defaultColor = (Color) comp.getClientProperty(KEY_DEFAULT_COLOR);
                if (defaultColor != null) {
                    comp.setForeground(defaultColor);
                }
            }
        }
    }

}
