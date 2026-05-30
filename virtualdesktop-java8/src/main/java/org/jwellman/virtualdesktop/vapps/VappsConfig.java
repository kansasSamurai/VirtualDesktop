package org.jwellman.virtualdesktop.vapps;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Root configuration container for virtual applications.
 * Loaded from config/vapps-config.json
 *
 * @author Rick Wellman
 */
public class VappsConfig {

    @JsonProperty("version")
    private String version = "1.0.0";

    @JsonProperty("defaultIcon")
    private String defaultIcon = "home";

    @JsonProperty("menuStructure")
    private List<MenuGroup> menuStructure = new ArrayList<MenuGroup>();

    @JsonProperty("desktopShortcuts")
    private List<DesktopShortcut> desktopShortcuts = new ArrayList<DesktopShortcut>();

    /**
     * Default constructor for Jackson deserialization
     */
    public VappsConfig() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDefaultIcon() {
        return defaultIcon;
    }

    public void setDefaultIcon(String defaultIcon) {
        this.defaultIcon = defaultIcon;
    }

    public List<MenuGroup> getMenuStructure() {
        return menuStructure;
    }

    public void setMenuStructure(List<MenuGroup> menuStructure) {
        this.menuStructure = menuStructure;
    }

    public List<DesktopShortcut> getDesktopShortcuts() {
        return desktopShortcuts;
    }

    public void setDesktopShortcuts(List<DesktopShortcut> desktopShortcuts) {
        this.desktopShortcuts = desktopShortcuts;
    }
}
