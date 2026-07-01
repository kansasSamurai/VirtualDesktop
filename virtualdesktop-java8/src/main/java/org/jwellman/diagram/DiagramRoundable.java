package org.jwellman.diagram;

/**
 * Interface for diagram components that support a variable corner radius.
 * A radius of 0 produces sharp corners; higher values round them.
 */
public interface DiagramRoundable {
    int getCornerRadius();
    void setCornerRadius(int radius);
}
