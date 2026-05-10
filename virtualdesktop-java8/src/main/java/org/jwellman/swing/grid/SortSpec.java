package org.jwellman.swing.grid;

/**
 * Immutable value object pairing a column key with a sort direction.
 * A {@code List<SortSpec>} represents the full multi-column sort specification;
 * index 0 is the primary sort, index 1 the secondary, and so on.
 */
public final class SortSpec {

    private final String    key;
    private final SortOrder order;

    public SortSpec(String key, SortOrder order) {
        this.key   = key;
        this.order = order;
    }

    public String getKey() {
        return key;
    }

    public SortOrder getOrder() {
        return order;
    }
}
