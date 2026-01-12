package org.jwellman.virtualdesktop.vapps;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for desktop shortcut icons.
 *
 * @author Rick Wellman
 */
public class DesktopShortcut {

    @JsonProperty("class")
    private String className;

    @JsonProperty("label")
    private String label; // Display label on desktop

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("enabled")
    private boolean enabled = true;

    /**
     * Default constructor for Jackson deserialization
     */
    public DesktopShortcut() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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
}
