package org.katacode.appstage.internal;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.katacode.appstage.NotificationService;
import org.katacode.appstage.ToastType;

/**
 * Minimal toast stack on the stage POPUP layer.
 */
public class DefaultNotificationService implements NotificationService {

    private static final int DEFAULT_DURATION_MS = 3000;
    private static final int TOAST_MARGIN = 12;
    private static final int TOAST_GAP = 8;
    private static final int TOAST_WIDTH = 280;

    private final JComponent popupLayer;
    private final List<ToastEntry> active = new ArrayList<ToastEntry>();

    public DefaultNotificationService(JComponent popupLayer) {
        this.popupLayer = popupLayer;
    }

    @Override
    public void showToast(String title, String message, ToastType type) {
        showInternal(title, message, type, DEFAULT_DURATION_MS);
    }

    @Override
    public void showToast(String message, int durationMillis) {
        showInternal(null, message, ToastType.INFO, durationMillis);
    }

    @Override
    public void clearAllToasts() {
        List<ToastEntry> copy = new ArrayList<ToastEntry>(active);
        for (ToastEntry entry : copy) {
            dismiss(entry);
        }
    }

    private void showInternal(String title, String message, ToastType type, int durationMillis) {
        final JPanel toast = buildToast(title, message, type);
        final ToastEntry entry = new ToastEntry(toast);
        active.add(entry);
        popupLayer.add(toast);
        layoutToasts();
        popupLayer.revalidate();
        popupLayer.repaint();

        int ms = durationMillis > 0 ? durationMillis : DEFAULT_DURATION_MS;
        entry.timer = new Timer(ms, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dismiss(entry);
            }
        });
        entry.timer.setRepeats(false);
        entry.timer.start();
    }

    private void dismiss(ToastEntry entry) {
        if (entry.timer != null) {
            entry.timer.stop();
        }
        if (!active.remove(entry)) {
            return;
        }
        popupLayer.remove(entry.panel);
        layoutToasts();
        popupLayer.revalidate();
        popupLayer.repaint();
    }

    private void layoutToasts() {
        int layerH = popupLayer.getHeight();
        int layerW = popupLayer.getWidth();
        if (layerH <= 0 || layerW <= 0) {
            return;
        }
        int y = layerH - TOAST_MARGIN;
        for (int i = active.size() - 1; i >= 0; i--) {
            JPanel toast = active.get(i).panel;
            int h = toast.getPreferredSize().height;
            y -= h;
            int x = Math.max(TOAST_MARGIN, layerW - TOAST_WIDTH - TOAST_MARGIN);
            toast.setBounds(x, y, TOAST_WIDTH, h);
            y -= TOAST_GAP;
        }
    }

    private JPanel buildToast(String title, String message, ToastType type) {
        Color accent = accentFor(type);
        JPanel toast = new JPanel(new GridBagLayout());
        toast.setOpaque(true);
        toast.setBackground(new Color(245, 245, 245));
        toast.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200)),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10))));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 0);

        if (title != null && title.length() > 0) {
            gbc.gridy = 0;
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            toast.add(titleLabel, gbc);
            gbc.gridy = 1;
            gbc.insets = new Insets(4, 0, 0, 0);
        } else {
            gbc.gridy = 0;
        }

        JLabel msgLabel = new JLabel("<html>" + escape(message) + "</html>");
        toast.add(msgLabel, gbc);
        toast.setSize(TOAST_WIDTH, toast.getPreferredSize().height);
        return toast;
    }

    private static Color accentFor(ToastType type) {
        if (type == null) {
            return new Color(0, 120, 215);
        }
        switch (type) {
            case SUCCESS:
                return new Color(16, 124, 16);
            case WARN:
                return new Color(157, 93, 0);
            case ERROR:
                return new Color(168, 0, 0);
            case INFO:
            default:
                return new Color(0, 120, 215);
        }
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Called when the popup layer is resized so stacked toasts stay anchored.
     */
    public void relayout() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    layoutToasts();
                }
            });
            return;
        }
        layoutToasts();
    }

    private static final class ToastEntry {
        final JPanel panel;
        Timer timer;

        ToastEntry(JPanel panel) {
            this.panel = panel;
        }
    }
}
