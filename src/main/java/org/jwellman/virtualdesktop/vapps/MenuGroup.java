package org.jwellman.virtualdesktop.vapps;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a menu group that can contain vapps and nested subgroups.
 * Supports hierarchical menu organization.
 *
 * @author Rick Wellman
 */
public class MenuGroup {

    @JsonProperty("type")
    private String type = "flat"; // "group" or "flat"

    @JsonProperty("label")
    private String label;

    @JsonProperty("mnemonic")
    private String mnemonic; // Single character

    @JsonProperty("groups")
    private List<MenuGroup> groups = new ArrayList<MenuGroup>();

    @JsonProperty("vapps")
    private List<VappConfig> vapps = new ArrayList<VappConfig>();

    /**
     * Default constructor for Jackson deserialization
     */
    public MenuGroup() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    public List<MenuGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<MenuGroup> groups) {
        this.groups = groups;
    }

    public List<VappConfig> getVapps() {
        return vapps;
    }

    public void setVapps(List<VappConfig> vapps) {
        this.vapps = vapps;
    }

    /**
     * Check if this menu group supports nested subgroups
     *
     * @return true if type is "group"
     */
    public boolean isGroupType() {
        return "group".equalsIgnoreCase(type);
    }
}
