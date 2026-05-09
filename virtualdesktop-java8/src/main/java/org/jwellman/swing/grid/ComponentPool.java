package org.jwellman.swing.grid;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;
import javax.swing.JComponent;

/**
 * Object pool for row components. Maintains a fixed-size supply of live
 * JComponents regardless of total row count; memory footprint is flat.
 */
public class ComponentPool {

    private final Supplier<JComponent> factory;
    private final Deque<JComponent> available = new ArrayDeque<>();

    public ComponentPool(Supplier<JComponent> factory) {
        this.factory = factory;
    }

    /**
     * Returns a component ready for use. Pulled from the pool if available
     * (after calling {@link Recyclable#prepareForReuse()}), otherwise created fresh.
     */
    public JComponent checkout() {
        if (!available.isEmpty()) {
            JComponent c = available.pop();
            if (c instanceof Recyclable) {
                ((Recyclable) c).prepareForReuse();
            }
            return c;
        }
        return factory.get();
    }

    /** Returns a component to the pool for later reuse. */
    public void release(JComponent c) {
        available.push(c);
    }

    public int availableCount() { return available.size(); }
}
