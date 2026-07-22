package org.jwellman.virtualdesktop.tools;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable catalog entry describing a tool that can be launched.
 *
 * <p>This is data only — not a Swing Action and not a running instance.
 * Launch surfaces adapt definitions; {@link ToolService} performs open.</p>
 */
public final class ToolDefinition {

    private final String id;
    private final String title;
    private final String iconKey;
    private final String className;
    private final Map<String, String> attrs;
    private final ToolLaunchKind launchKind;
    private final String command;
    private final String workingDirectory;
    private final boolean waitForCompletion;

    private ToolDefinition(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.iconKey = builder.iconKey;
        this.className = builder.className;
        if (builder.attrs == null || builder.attrs.isEmpty()) {
            this.attrs = Collections.emptyMap();
        } else {
            this.attrs = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.attrs));
        }
        this.launchKind = builder.launchKind;
        this.command = builder.command;
        this.workingDirectory = builder.workingDirectory;
        this.waitForCompletion = builder.waitForCompletion;
    }

    /**
     * Stable id for an internal tool: class name, plus attrs fingerprint when present.
     */
    public static String internalId(String className, Map<String, String> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return className;
        }
        return className + "#" + stableAttrsKey(attrs);
    }

    /**
     * Stable id for an external tool.
     */
    public static String externalId(String name) {
        return "external:" + name;
    }

    private static String stableAttrsKey(Map<String, String> attrs) {
        java.util.List<String> keys = new java.util.ArrayList<String>(attrs.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(attrs.get(key));
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIconKey() {
        return iconKey;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public ToolLaunchKind getLaunchKind() {
        return launchKind;
    }

    public String getCommand() {
        return command;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    public boolean isExternal() {
        return launchKind == ToolLaunchKind.EXTERNAL;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private String title;
        private String iconKey;
        private String className;
        private Map<String, String> attrs;
        private ToolLaunchKind launchKind = ToolLaunchKind.INTERNAL;
        private String command;
        private String workingDirectory;
        private boolean waitForCompletion;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder iconKey(String iconKey) {
            this.iconKey = iconKey;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder attrs(Map<String, String> attrs) {
            this.attrs = attrs;
            return this;
        }

        public Builder launchKind(ToolLaunchKind launchKind) {
            this.launchKind = launchKind;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder waitForCompletion(boolean waitForCompletion) {
            this.waitForCompletion = waitForCompletion;
            return this;
        }

        public ToolDefinition build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("ToolDefinition id is required");
            }
            if (title == null || title.isEmpty()) {
                throw new IllegalStateException("ToolDefinition title is required");
            }
            if (launchKind == null) {
                throw new IllegalStateException("ToolDefinition launchKind is required");
            }
            if (launchKind == ToolLaunchKind.INTERNAL) {
                if (className == null || className.isEmpty()) {
                    throw new IllegalStateException("INTERNAL ToolDefinition requires className");
                }
            } else if (launchKind == ToolLaunchKind.EXTERNAL) {
                if (command == null || command.isEmpty()) {
                    throw new IllegalStateException("EXTERNAL ToolDefinition requires command");
                }
            }
            return new ToolDefinition(this);
        }
    }

    @Override
    public String toString() {
        return "ToolDefinition{id=" + id + ", title=" + title + ", kind=" + launchKind + "}";
    }

}
