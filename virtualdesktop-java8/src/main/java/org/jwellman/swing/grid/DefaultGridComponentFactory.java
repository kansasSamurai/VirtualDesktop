package org.jwellman.swing.grid;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Convenience implementation of {@link GridComponentFactory} backed by a SmartGrid.
 *
 * Maintains a {@link ScriptSpec} registry keyed by fnd-type. When
 * {@link #create(String, String, String, String, ScriptBridge)} is called a second
 * time with the same key, it updates the shared spec in-place rather than replacing
 * the pool — all live instances of that type (in slots and in the pool) see the new
 * scripts on their next {@code bind()} call, enabling live renderer swaps with no
 * mixed-state artifacts.
 */
public class DefaultGridComponentFactory implements GridComponentFactory {

    private final SmartGrid               grid;
    private final bsh.Interpreter         interpreter;
    private final Map<String, ScriptSpec> specRegistry = new HashMap<>();

    /** Standalone mode — each ScriptableRecyclable gets its own interpreter. */
    public DefaultGridComponentFactory(SmartGrid grid) {
        this(grid, null);
    }

    /**
     * Shared-interpreter mode — all scripted row instances execute scripts in
     * {@code interpreter}'s context. Pass {@code null} for standalone mode.
     *
     * <p>Typical BeanShell console usage:
     * <pre>
     *   DefaultGridComponentFactory gridFactory =
     *       new DefaultGridComponentFactory(grid, _interpreter);
     * </pre>
     */
    public DefaultGridComponentFactory(SmartGrid grid, bsh.Interpreter interpreter) {
        this.grid        = grid;
        this.interpreter = interpreter;
    }

    @Override
    public void register(String fndType, Supplier<JComponent> supplier) {
        grid.registerRowRenderer(fndType, supplier);
    }

    /**
     * BeanShell-friendly registration: layout, bind, and reset as separate strings.
     *
     * <p>First call for a given {@code fndType}: creates a {@link ScriptSpec} and
     * registers a new pool supplier.
     *
     * <p>Subsequent calls with the same {@code fndType}: updates the existing
     * {@link ScriptSpec} in place — no pool replacement, no slot recycling needed.
     * All live instances see the new scripts on their next {@code bind()} call.
     * Call {@code model.notifyDataChanged()} after to trigger a repaint.
     *
     * <pre>
     *   ScriptBridge bridge = new ScriptBridge(grid, _interpreter);
     *   gridFactory.create("delete-event", layout, bindA, reset, bridge);
     *   // live swap later:
     *   gridFactory.create("delete-event", layout, bindB, reset, bridge);
     *   model.notifyDataChanged();
     * </pre>
     */
    public void create(String fndType, final String layoutXml,
                       final String bindScript, final String resetScript,
                       final ScriptBridge bridge) {
        ScriptSpec existing = specRegistry.get(fndType);
        if (existing != null) {
            // Update scripts in-place — all live instances see the change immediately
            existing.bindScript  = bindScript;
            existing.resetScript = resetScript;
            return;
        }

        ScriptSpec spec = new ScriptSpec(bindScript, resetScript);
        specRegistry.put(fndType, spec);

        final ScriptSpec capturedSpec  = spec;
        final int[]      sharedWidths  = bridge.getColumnWidths();
        final bsh.Interpreter bsh      = bridge.getInterpreter();

        register(fndType, new Supplier<JComponent>() {
            @Override
            public JComponent get() {
                JPanel panel;
                try {
                    panel = RowBlueprint.buildPanel(layoutXml);
                } catch (Exception e) {
                    System.err.println("RowBlueprint.buildPanel failed for fnd-type="
                            + fndType + ": " + e.getMessage());
                    panel = new JPanel();
                }
                return new ScriptableRecyclable(panel, sharedWidths, bsh, capturedSpec);
            }
        });
    }

    /**
     * XML-embedded-script registration: layout and scripts are all inside {@code xml}
     * as {@code <script name="bind">} and {@code <script name="prepare">} elements.
     * Used by {@link org.jwellman.demo.SmartGridDemo} and the embedded-blueprint path.
     *
     * <p>Note: this path creates its own per-call ScriptSpec and does not participate
     * in the shared spec registry — re-calling with the same key replaces the pool.
     */
    public void create(String fndType, final String xml) {
        final String bindScript;
        final String prepareScript;
        try {
            bindScript    = RowBlueprint.extractScript(xml, "bind");
            prepareScript = RowBlueprint.extractScript(xml, "prepare");
        } catch (Exception e) {
            throw new RuntimeException("RowBlueprint parse failed for fnd-type=" + fndType, e);
        }

        final int[]           sharedWidths = grid.getColumnWidths();
        final bsh.Interpreter bsh          = this.interpreter;
        final ScriptSpec      spec         = new ScriptSpec(bindScript, prepareScript);

        register(fndType, new Supplier<JComponent>() {
            @Override
            public JComponent get() {
                JPanel panel;
                try {
                    panel = RowBlueprint.buildPanel(xml);
                } catch (Exception e) {
                    System.err.println("RowBlueprint.buildPanel failed for fnd-type="
                            + fndType + ": " + e.getMessage());
                    panel = new JPanel();
                }
                return new ScriptableRecyclable(panel, sharedWidths, bsh, spec);
            }
        });
    }
}
