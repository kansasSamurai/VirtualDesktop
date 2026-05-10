package org.jwellman.swing.grid;

import java.util.function.Supplier;
import javax.swing.JComponent;

/**
 * Registration interface for row-level component dispatch.
 *
 * When a {@link GridRow} carries a {@code fnd-type} tag, SmartGrid looks up that
 * tag in the registry and uses the registered supplier to obtain the component from
 * a type-specific pool. Rows with no {@code fnd-type} tag continue to use the
 * default {@link StandardRowPanel}.
 *
 * Phase 9b (BeanShell / ScriptableRecyclable) will extend this interface with a
 * {@code create(String)} method for scripted instantiation.
 */
public interface GridComponentFactory {
    /**
     * Registers a row component supplier for the given {@code fnd-type} value.
     * The supplier is called to create new pool instances; the component must
     * implement {@link Recyclable}.
     */
    void register(String fndType, Supplier<JComponent> supplier);
}
