package org.katacode.agents.auditor.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jiconfont.icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;

/**
 * Title bar across the top of the tool: brand and standard version on the left,
 * workspace actions on the right. Visual rough-in only; none of the controls do
 * anything yet.
 */
@SuppressWarnings("serial")
public class AuditorToolBar extends JPanel {

    private static final int HEIGHT = 57;
    private static final int CONTROL_HEIGHT = 34;
    private static final int BRAND_GAP = 12;
    private static final int ACTION_GAP = 8;

    /** Mocked until standard profiles exist. */
    private static final String STANDARD_VERSION = "v2026.11";

    public AuditorToolBar() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(AuditorTheme.toolBarBackground());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AuditorTheme.lineSubtle()),
                BorderFactory.createEmptyBorder(0, 18, 0, 12)));

        addBrand();
        add(Box.createHorizontalGlue());
        addActions();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = HEIGHT;

        return size;
    }

    private void addBrand() {
        JLabel title = new JLabel("AI PROJECT WORKSPACE", new BrandIcon(), JLabel.LEADING);
        title.setIconTextGap(BRAND_GAP);
        title.setFont(AuditorTheme.uiFont(Font.PLAIN, 16f)
                .deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.02f)));
        title.setForeground(AuditorTheme.textPrimary());

        JPanel separator = new JPanel();
        separator.setBackground(AuditorTheme.lineStrong());
        Dimension separatorSize = new Dimension(1, 22);
        separator.setPreferredSize(separatorSize);
        separator.setMinimumSize(separatorSize);
        separator.setMaximumSize(separatorSize);

        JLabel standard = new JLabel("[Standard: " + STANDARD_VERSION + "]");
        standard.setFont(AuditorTheme.uiFont(Font.PLAIN, 15f));
        standard.setForeground(AuditorTheme.textSecondary());
        // Lets the label truncate when the window is narrow
        standard.setMinimumSize(new Dimension(0, 0));

        addCentered(title);
        addGap(BRAND_GAP);
        addCentered(separator);
        addGap(BRAND_GAP);
        addCentered(standard);
        addGap(BRAND_GAP);
    }

    private void addActions() {
        addCentered(new RoundedButton("Scan Roots", CONTROL_HEIGHT));
        addGap(ACTION_GAP);
        addCentered(new RoundedButton("Rescan All", CONTROL_HEIGHT));
        addGap(ACTION_GAP);
        addCentered(new RoundedButton("Standard Editor", CONTROL_HEIGHT));
        addGap(ACTION_GAP);
        addCentered(new SearchField());
        addGap(ACTION_GAP);
        addCentered(createSettingsButton());
    }

    private JButton createSettingsButton() {
        JButton button = new JButton(IconFontSwing.buildIcon(
                GoogleMaterialDesignIcons.SETTINGS, 19, AuditorTheme.textSecondary()));
        button.setToolTipText("Settings");
        button.getAccessibleContext().setAccessibleName("Settings");
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Dimension size = new Dimension(CONTROL_HEIGHT, CONTROL_HEIGHT);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);

        return button;
    }

    private void addCentered(JComponent component) {
        component.setAlignmentY(Component.CENTER_ALIGNMENT);
        add(component);
    }

    private void addGap(int width) {
        add(Box.createHorizontalStrut(width));
    }
}
