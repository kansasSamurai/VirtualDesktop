package org.jwellman.swing.grid;

public interface Recyclable {
    /** Reset the component to a blank factory-default state. */
    void prepareForReuse();

    /**
     * Bind data from the given row. {@code rowIndex} is provided so
     * implementations can apply alternating-row styling.
     */
    void bind(GridRow row, int rowIndex);
}
