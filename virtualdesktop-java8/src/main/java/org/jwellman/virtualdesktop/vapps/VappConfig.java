package org.jwellman.virtualdesktop.vapps;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a single virtual application.
 *
 * @author Rick Wellman
 */
public class VappConfig {

    @JsonProperty("class")
    private String className; // Fully qualified class name

    @JsonProperty("title")
    private String title; // Optional display title

    @JsonProperty("icon")
    private String icon; // Icon key without size suffix

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("desktopOnly")
    private boolean desktopOnly = false;

    @JsonProperty("args")
    private Map<String, String> args;

    @JsonProperty("attrs")
    private Map<String, String> attrs;

    /**
     * Default constructor for Jackson deserialization
     */
    public VappConfig() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDesktopOnly() {
        return desktopOnly;
    }

    public void setDesktopOnly(boolean desktopOnly) {
        this.desktopOnly = desktopOnly;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public void setArgs(Map<String, String> args) {
        this.args = args;
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, String> attrs) {
        this.attrs = attrs;
    }
}
