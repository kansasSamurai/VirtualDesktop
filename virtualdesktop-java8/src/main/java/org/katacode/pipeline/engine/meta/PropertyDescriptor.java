package org.katacode.pipeline.engine.meta;

/**
 * Defines a structural parameter metadata descriptor for dynamic UI form generation.
 */
public class PropertyDescriptor {
    public enum PropertyType { TEXT, COMPONENT_CHOICE, BOOLEAN }

    private final String key;
    private final String displayName;
    private final PropertyType type;
    private final String[] options; // Only utilized for COMPONENT_CHOICE lists

    public PropertyDescriptor(String key, String displayName, PropertyType type) {
        this(key, displayName, type, null);
    }

    public PropertyDescriptor(String key, String displayName, PropertyType type, String[] options) {
        this.key = key;
        this.displayName = displayName;
        this.type = type;
        this.options = options;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public PropertyType getType() { return type; }
    public String[] getOptions() { return options; }

}
