package org.katacode.agents.auditor.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.katacode.agents.auditor.model.RegisteredProject;

/**
 * Posture of the selected project. So far only the header: project name and
 * folder path.
 */
@SuppressWarnings("serial")
public class PosturePanel extends JPanel {

    private static final String TITLE_PREFIX = "PROJECT POSTURE";

    private final JLabel titleLabel = new JLabel();
    private final JLabel folderLabel = new JLabel();

    public PosturePanel() {
        super(new BorderLayout());

        setBackground(AuditorTheme.contentBackground());
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        titleLabel.setFont(AuditorTheme.uiFont(Font.BOLD, 26f));
        titleLabel.setForeground(AuditorTheme.textPrimary());

        folderLabel.setFont(AuditorTheme.monoFont(14));
        folderLabel.setForeground(AuditorTheme.textSecondary());

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AuditorTheme.lineSubtle()),
                BorderFactory.createEmptyBorder(0, 0, 16, 0)));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(folderLabel, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        showProject(null);
    }

    /**
     * Displays the given project.
     *
     * @param project the selected project, or null when there is none
     */
    public void showProject(RegisteredProject project) {
        if (project == null) {
            titleLabel.setText(TITLE_PREFIX);
            folderLabel.setText("No project selected");
            folderLabel.setToolTipText(null);
        } else {
            titleLabel.setText(TITLE_PREFIX + ": " + project.getTitle());
            folderLabel.setText(project.getFolder());
            folderLabel.setToolTipText(project.getFolder());
        }
    }
}
