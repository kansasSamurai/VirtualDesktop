package org.jwellman.swing.grid;

/**
 * Connects {@link ScriptableRecyclable} pool instances to the grid's shared
 * state and BeanShell interpreter context.
 *
 * Create one bridge per grid and share it across all pool instances registered
 * for that grid. The {@code columnWidths} reference is the shared mutable array
 * owned by {@link SmartGrid}, so in-place updates made by SmartGrid on resize
 * are automatically visible to all pool instances.
 *
 * <p>Typical BeanShell console usage:
 * <pre>
 *   ScriptBridge bridge = new ScriptBridge(grid, _interpreter);
 *   gridFactory.register("delete-event", () -&gt;
 *       new ScriptableRecyclable(layout, bind, reset, bridge));
 * </pre>
 */
public class ScriptBridge {

    private final int[]           columnWidths;
    private final bsh.Interpreter interpreter;

    public ScriptBridge(SmartGrid grid, bsh.Interpreter interpreter) {
        this.columnWidths = grid.getColumnWidths();
        this.interpreter  = interpreter;
    }

    public int[] getColumnWidths() {
        return columnWidths;
    }

    public bsh.Interpreter getInterpreter() {
        return interpreter;
    }
}
