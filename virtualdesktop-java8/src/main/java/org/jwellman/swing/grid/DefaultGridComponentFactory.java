package org.jwellman.swing.grid;

import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Convenience implementation of {@link GridComponentFactory} backed by a SmartGrid.
 *
 * Also provides {@link #create(String, String)} for registering a row renderer
 * defined by an XML blueprint string, integrating {@link RowBlueprint} and
 * {@link ScriptableRecyclable}.
 *
 * <p>When constructed with a {@code bsh.Interpreter} (e.g. the VirtualDesktop
 * console's {@code _interpreter}), all scripted row instances share that
 * interpreter context — scripts can reference variables already defined in the
 * console session. When no interpreter is supplied, each pool instance gets its
 * own private interpreter (suitable for standalone / demo use).
 */
public class DefaultGridComponentFactory implements GridComponentFactory {

    private final SmartGrid        grid;
    private final bsh.Interpreter  interpreter;

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
     *   DefaultGridComponentFactory factory =
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
     * Parses {@code xml} as a Blueprint DSL, extracts embedded BeanShell scripts,
     * and registers a row renderer for {@code fndType} that produces
     * {@link ScriptableRecyclable} instances.
     *
     * Each pool checkout re-parses the XML to produce an independent component
     * tree — Swing prohibits a single JPanel from being added to multiple parents.
     *
     * @throws RuntimeException wrapping any XML parse failure
     */
    /**
     * BeanShell-friendly registration: layout, bind, and reset as separate strings
     * with a {@link ScriptBridge} providing interpreter + column widths.
     *
     * <p>Preferred for console use — no {@code Supplier} anonymous class needed:
     * <pre>
     *   ScriptBridge bridge = new ScriptBridge(grid, _interpreter);
     *   gridFactory.create("delete-event", layout, bind, reset, bridge);
     * </pre>
     *
     * Can be called again with the same {@code fndType} to swap the renderer live;
     * call {@code model.notifyDataChanged()} after to force a refresh.
     */
    public void create(String fndType, final String layoutXml,
                       final String bindScript, final String resetScript,
                       final ScriptBridge bridge) {
        register(fndType, new Supplier<JComponent>() {
            @Override
            public JComponent get() {
                ScriptableRecyclable sr =
                    new ScriptableRecyclable(layoutXml, bindScript, resetScript, bridge);
                return sr;
            }
        });
    }

    /**
     * XML-embedded-script registration: layout and scripts are all inside {@code xml}.
     * Used by {@link org.jwellman.demo.SmartGridDemo} and the {@code <script>}-embedded
     * blueprint path.
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
                ScriptableRecyclable sr = new ScriptableRecyclable(panel, sharedWidths, bsh);
                sr.setBindScript(bindScript);
                sr.setPrepareScript(prepareScript);
                return sr;
            }
        });
    }
}
