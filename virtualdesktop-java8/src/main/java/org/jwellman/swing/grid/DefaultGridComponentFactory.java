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
     * Adapter-based registration: the {@link RowScript} object handles bind and
     * reset as real BeanShell methods rather than eval'd strings.
     *
     * <p>Preferred for complex rendering — no string escaping, full closure over
     * the console namespace, readable error messages.
     *
     * <p>First call for a given {@code fndType}: registers the pool.
     * Subsequent calls with the same key: swaps the adapter in the shared
     * {@link ScriptSpec} — all live instances use the new adapter immediately.
     * Call {@code model.notifyDataChanged()} after to trigger a repaint.
     *
     * <pre>
     *   RowScript r = new RowScript() {
     *       public void bind(ScriptableRecyclable self, GridRow row, int rowIndex) {
     *           self.setBackground(new Color(0x8B0000));
     *           ((JLabel) self.find("ts")).setText(row.get("timestamp").toString());
     *       }
     *       public void reset(ScriptableRecyclable self) { self.setBackground(null); }
     *   };
     *   gridFactory.create("delete-event", layout, r, bridge);
     * </pre>
     */
    public void create(String fndType, final String layoutXml,
                       final RowScript adapter, final ScriptBridge bridge) {
        ScriptSpec existing = specRegistry.get(fndType);
        if (existing != null) {
            existing.adapter = adapter;
            return;
        }

        ScriptSpec spec = new ScriptSpec(adapter);
        specRegistry.put(fndType, spec);

        final ScriptSpec      capturedSpec = spec;
        final int[]           sharedWidths = bridge.getColumnWidths();
        final bsh.Interpreter bsh          = bridge.getInterpreter();

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
     * String-based registration: layout, bind, and reset as separate strings.
     *
     * <p>Useful for simple one-line scripts defined inline. For multi-line
     * rendering logic, prefer
     * {@link #create(String, String, RowScript, ScriptBridge)}.
     *
     * <p>First call for a given {@code fndType}: registers the pool.
     * Subsequent calls with the same key: updates the shared {@link ScriptSpec}
     * in place — all live instances see the new scripts on their next bind().
     * Call {@code model.notifyDataChanged()} after to trigger a repaint.
     */
    public void create(String fndType, final String layoutXml,
                       final String bindScript, final String resetScript,
                       final ScriptBridge bridge) {
        ScriptSpec existing = specRegistry.get(fndType);
        if (existing != null) {
            existing.adapter     = null;
            existing.bindScript  = bindScript;
            existing.resetScript = resetScript;
            return;
        }

        ScriptSpec spec = new ScriptSpec(bindScript, resetScript);
        specRegistry.put(fndType, spec);

        final ScriptSpec      capturedSpec = spec;
        final int[]           sharedWidths = bridge.getColumnWidths();
        final bsh.Interpreter bsh          = bridge.getInterpreter();

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
