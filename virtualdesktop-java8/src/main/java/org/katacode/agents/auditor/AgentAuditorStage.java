package org.katacode.agents.auditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.katacode.agents.auditor.model.RegisteredProject;
import org.katacode.agents.auditor.ui.AuditorTheme;
import org.katacode.agents.auditor.ui.AuditorToolBar;
import org.katacode.agents.auditor.ui.PostureArea;
import org.katacode.agents.auditor.ui.ProjectSidebar;
import org.katacode.appstage.PanelApplicationStage;

/**
 * Main component of the Agent Auditor tool: an application stage holding the
 * toolbar, the registered project list and, to its right, the selected project's
 * posture area.
 */
@SuppressWarnings("serial")
public class AgentAuditorStage extends PanelApplicationStage {

    private static final String MAIN_CARD = "main";
    private static final int SIDEBAR_WIDTH = 336;
    private static final int SIDEBAR_MIN_WIDTH = 260;
    private static final int DIVIDER_SIZE = 4;

    /**
     * Creates the stage and shows the main card.
     *
     * @param projects the registered projects to list; may be empty, not null
     */
    public AgentAuditorStage(List<RegisteredProject> projects) {
        super();
        addCard(MAIN_CARD, createMainCard(projects));
        showCard(MAIN_CARD);
    }

    private JPanel createMainCard(List<RegisteredProject> projects) {
        ProjectSidebar sidebar = new ProjectSidebar(projects);
        sidebar.setMinimumSize(new Dimension(SIDEBAR_MIN_WIDTH, 0));

        PostureArea posture = new PostureArea();
        // Lets the divider move freely; long titles and paths are truncated
        posture.setMinimumSize(new Dimension(0, 0));
        sidebar.setSelectionCallback(posture::showProject);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, posture);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerLocation(SIDEBAR_WIDTH);
        split.setDividerSize(DIVIDER_SIZE);
        split.setBackground(AuditorTheme.contentBackground());

        JPanel root = new JPanel(new BorderLayout());
        root.add(new AuditorToolBar(), BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);

        return root;
    }
}
