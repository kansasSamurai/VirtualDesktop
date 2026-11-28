package org.katacode.agents.auditor.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jiconfont.icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;

/**
 * Trail of crumbs across the top of the posture area. Earlier crumbs are links,
 * the last one is the current view, and "Back" returns to the root. At the root
 * there is no Back button.
 */
@SuppressWarnings("serial")
class BreadcrumbBar extends JPanel {

    private static final int HEIGHT = 41;
    private static final int GAP = 10;
    private static final int BACK_HEIGHT = 28;
    private static final String BACK_TEXT = "Back to Posture";

    private final List<Crumb> crumbs = new ArrayList<>();
    private final Consumer<String> navigation;

    /**
     * @param navigation receives the card name to show when the user navigates
     *                   back through the bar
     */
    BreadcrumbBar(Consumer<String> navigation) {
        this.navigation = navigation;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(AuditorTheme.breadcrumbBackground());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AuditorTheme.lineSubtle()),
                BorderFactory.createEmptyBorder(0, 28, 0, 28)));
    }

    /** Discards the trail and starts again with a single root crumb. */
    void reset(String label, String card) {
        crumbs.clear();
        push(label, card);
    }

    /** Appends a crumb as the current view. Does not call the navigation callback. */
    void push(String label, String card) {
        crumbs.add(new Crumb(label, card));
        rebuild();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = HEIGHT;

        return size;
    }

    private void popTo(int index) {
        while (crumbs.size() > index + 1) {
            crumbs.remove(crumbs.size() - 1);
        }
        rebuild();
        navigation.accept(crumbs.get(index).card);
    }

    private void rebuild() {
        removeAll();

        int last = crumbs.size() - 1;
        if (last > 0) {
            RoundedButton back = new RoundedButton(BACK_TEXT, BACK_HEIGHT);
            back.setFont(AuditorTheme.uiFont(Font.BOLD, 14f));
            back.setIcon(IconFontSwing.buildIcon(
                    GoogleMaterialDesignIcons.CHEVRON_LEFT, 16, AuditorTheme.textSecondary()));
            back.setIconTextGap(4);
            back.addActionListener(popAction(0));
            addCentered(back);
            add(Box.createHorizontalStrut(GAP));
        }

        for (int i = 0; i <= last; i++) {
            if (i > 0) {
                add(Box.createHorizontalStrut(GAP));
                addCentered(new JLabel(IconFontSwing.buildIcon(
                        GoogleMaterialDesignIcons.CHEVRON_RIGHT, 16, AuditorTheme.textMuted())));
                add(Box.createHorizontalStrut(GAP));
            }
            if (i < last) {
                LinkButton link = new LinkButton(crumbs.get(i).label);
                link.addActionListener(popAction(i));
                addCentered(link);
            } else {
                JLabel current = new JLabel(crumbs.get(i).label);
                current.setFont(AuditorTheme.uiFont(Font.BOLD, 14f));
                current.setForeground(AuditorTheme.textPrimary());
                addCentered(current);
            }
        }

        revalidate();
        repaint();
    }

    private void addCentered(JComponent component) {
        component.setAlignmentY(Component.CENTER_ALIGNMENT);
        add(component);
    }

    private ActionListener popAction(final int index) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popTo(index);
            }
        };
    }

    private static final class Crumb {

        private final String label;
        private final String card;

        private Crumb(String label, String card) {
            this.label = label;
            this.card = card;
        }
    }
}
