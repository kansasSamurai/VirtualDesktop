package org.katacode.agents.auditor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One explicitly registered project. Status and count are mocked in the
 * configuration until a scanner exists.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisteredProject {

    private String title = "";
    private String folder = "";
    private String status;
    private int count;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** Number of findings behind a warn or drift status. */
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @JsonIgnore
    public ProjectStatus getProjectStatus() {
        return ProjectStatus.parse(status);
    }
}
