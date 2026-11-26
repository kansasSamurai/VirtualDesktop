package org.jwellman.virtualdesktop.vapps;

import org.katacode.agents.auditor.AgentAuditorStage;

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
        this.setContent(new AgentAuditorStage());
    }
}
