package org.jwellman.virtualdesktop.vapps;

import java.util.ArrayList;
import java.util.List;

/**
 * Container class for external application configurations.
 * Used for JSON deserialization of the external apps configuration file.
 *
 * @author Rick Wellman
 */
public class ExternalAppsConfig {

    private List<ExternalAppConfig> externalApps = new ArrayList<>();

    // Default constructor required for Jackson
    public ExternalAppsConfig() {
    }

    /**
     * @return the externalApps
     */
    public List<ExternalAppConfig> getExternalApps() {
        return externalApps;
    }

    /**
     * @param externalApps the externalApps to set
     */
    public void setExternalApps(List<ExternalAppConfig> externalApps) {
        this.externalApps = externalApps;
    }
}
