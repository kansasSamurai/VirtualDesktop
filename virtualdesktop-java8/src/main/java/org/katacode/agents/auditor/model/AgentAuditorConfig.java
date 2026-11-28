package org.katacode.agents.auditor.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The "agentAuditor" section of config/vapps-config.json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentAuditorConfig {

    private List<RegisteredProject> projects = new ArrayList<>();

    public List<RegisteredProject> getProjects() {
        return projects;
    }

    public void setProjects(List<RegisteredProject> projects) {
        this.projects = projects == null ? new ArrayList<RegisteredProject>() : projects;
    }
}
