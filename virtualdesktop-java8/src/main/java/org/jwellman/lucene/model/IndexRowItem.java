package org.jwellman.lucene.model;

/**
 * Presentation wrapper for one row in the Lucene management sidebar SmartGrid.
 *
 * <p>Combines the static {@link DirectorySandboxConfig} (user-defined settings)
 * with the live {@link SandboxRuntimeState} (background thread telemetry) so
 * cell renderers can display both at once without coupling to either layer.</p>
 */
public class IndexRowItem {

    private final DirectorySandboxConfig config;
    private final SandboxRuntimeState runtimeState;

    public IndexRowItem(DirectorySandboxConfig config, SandboxRuntimeState runtimeState) {
        this.config = config;
        this.runtimeState = runtimeState;
    }

    public DirectorySandboxConfig getConfig() {
        return config;
    }

    public SandboxRuntimeState getRuntimeState() {
        return runtimeState;
    }

    @Override
    public String toString() {
        return config.getDisplayName();
    }
}
