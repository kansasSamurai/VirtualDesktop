package org.jwellman.virtualdesktop.vapps;

import java.util.Collections;
import java.util.List;

import org.katacode.agents.auditor.AgentAuditorStage;
import org.katacode.agents.auditor.model.RegisteredProject;

/**
 * Launch recipe for the Agent Auditor tool; the UI lives in
 * {@link AgentAuditorStage}.
 */
public class SpecAgentAuditor extends VirtualAppSpec {

    public SpecAgentAuditor() {
        super();
        this.setTitle("Agent Auditor");
        this.setWidth(1000);
        this.setHeight(700);
        this.setContent(new AgentAuditorStage(registeredProjects()));
    }

    private static List<RegisteredProject> registeredProjects() {
        VappsConfig config = ActionFactory.getVappsConfig();
        if (config == null) {
            return Collections.emptyList();
        }

        return config.getAgentAuditor().getProjects();
    }
}
