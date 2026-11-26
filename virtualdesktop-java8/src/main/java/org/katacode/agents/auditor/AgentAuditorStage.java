package org.katacode.agents.auditor;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.katacode.appstage.PanelApplicationStage;

/**
 * Main component of the Agent Auditor tool: an application stage holding the
 * tool's content. Currently a shell with a placeholder card.
 */
@SuppressWarnings("serial")
public class AgentAuditorStage extends PanelApplicationStage {

    private static final String MAIN_CARD = "main";

    /**
     * Creates the stage and shows the main card.
     */
    public AgentAuditorStage() {
        super();
        addCard(MAIN_CARD, createMainCard());
        showCard(MAIN_CARD);
    }

    private JPanel createMainCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(new JLabel("Agent Auditor: content coming soon", SwingConstants.CENTER),
                BorderLayout.CENTER);

        return root;
    }
}
