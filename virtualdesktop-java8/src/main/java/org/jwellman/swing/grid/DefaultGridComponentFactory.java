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
 */
public class DefaultGridComponentFactory implements GridComponentFactory {

    private final SmartGrid grid;

    public DefaultGridComponentFactory(SmartGrid grid) {
        this.grid = grid;
    }

    @Override
    public void register(String fndType, Supplier<JComponent> supplier) {
        grid.registerRowRenderer(fndType, supplier);
    }

    /**
     * Parses {@code xml} as a Blueprint DSL, extracts any embedded BeanShell
     * scripts, and registers a row renderer for {@code fndType} that produces
     * {@link ScriptableRecyclable} instances.
     *
     * Each pool checkout re-parses the XML to produce an independent component
     * tree — Swing prohibits a single JPanel from being added to multiple parents.
     *
     * @throws RuntimeException wrapping any XML parse failure
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

        final int[] sharedWidths = grid.getColumnWidths();

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
                ScriptableRecyclable sr = new ScriptableRecyclable(panel, sharedWidths);
                sr.setBindScript(bindScript);
                sr.setPrepareScript(prepareScript);
                return sr;
            }
        });
    }
}
