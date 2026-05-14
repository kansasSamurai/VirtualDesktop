package org.jwellman.swing.grid;

/**
 * Mutable script holder shared by all {@link ScriptableRecyclable} instances
 * of the same fnd-type.
 *
 * Because all instances share a reference to the same ScriptSpec, updating the
 * scripts here takes effect on the next {@code bind()} call for every live
 * instance — whether currently in a slot or waiting in the pool — enabling live
 * renderer swaps without replacing the pool or recycling slots.
 *
 * All access is on the Swing EDT so no synchronization is needed.
 */
class ScriptSpec {

    // String-based path: scripts evaluated via BeanShell interpreter
    String    bindScript;
    String    resetScript;

    // Adapter-based path: methods dispatched directly on the RowScript object
    // Takes priority over string scripts when non-null.
    RowScript adapter;

    ScriptSpec(String bindScript, String resetScript) {
        this.bindScript  = bindScript;
        this.resetScript = resetScript;
    }

    ScriptSpec(RowScript adapter) {
        this.adapter = adapter;
    }
}
