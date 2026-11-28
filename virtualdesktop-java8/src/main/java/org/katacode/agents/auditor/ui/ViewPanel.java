package org.katacode.agents.auditor.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Base for the views shown below the breadcrumb: a header with a title and a
 * monospaced path, separated from the content by a rule.
 */
@SuppressWarnings("serial")
class ViewPanel extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel pathLabel = new JLabel();

    ViewPanel() {
        super(new BorderLayout());

        setBackground(AuditorTheme.contentBackground());
        setBorder(BorderFactory.createEmptyBorder(18, 28, 28, 28));

        titleLabel.setFont(AuditorTheme.uiFont(Font.BOLD, 26f));
        titleLabel.setForeground(AuditorTheme.textPrimary());

        pathLabel.setFont(AuditorTheme.monoFont(14));
        pathLabel.setForeground(AuditorTheme.textSecondary());

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AuditorTheme.lineSubtle()),
                BorderFactory.createEmptyBorder(0, 0, 16, 0)));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(pathLabel, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
    }

    final void showHeader(String title, String path) {
        titleLabel.setText(title);
        pathLabel.setText(path);
        pathLabel.setToolTipText(null);
    }

    final void setPathToolTip(String text) {
        pathLabel.setToolTipText(text);
    }
}
