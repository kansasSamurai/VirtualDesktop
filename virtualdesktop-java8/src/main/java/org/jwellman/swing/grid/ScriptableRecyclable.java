package org.jwellman.swing.grid;

import javax.swing.JPanel;

/**
 * A row component whose bind/reset lifecycle is driven by BeanShell scripts.
 *
 * The component tree inside the panel is built from an XML blueprint by
 * {@link RowBlueprint}. Each pool instance holds a dedicated BeanShell
 * interpreter to prevent cross-contamination between concurrently-bound slots.
 *
 * Scripts receive two variables in scope:
 *   panel    — the blueprint JPanel (use ComponentFinder.find(panel, "id") to reach children)
 *   row      — the GridRow being rendered (bind script only)
 *   rowIndex — the row index (bind script only)
 */
public class ScriptableRecyclable extends JPanel implements Recyclable {

    private final JPanel         blueprintPanel;
    private final int[]          columnWidths;
    private       String         bindScript;
    private       String         prepareScript;
    private       bsh.Interpreter interpreter;

    public ScriptableRecyclable(JPanel blueprintPanel, int[] columnWidths) {
        this.blueprintPanel = blueprintPanel;
        this.columnWidths   = columnWidths;
        setLayout(null);
        setOpaque(true);
        add(blueprintPanel);
        initInterpreter();
    }

    public void setBindScript(String script) {
        this.bindScript = script;
    }

    public void setPrepareScript(String script) {
        this.prepareScript = script;
    }

    @Override
    public void bind(GridRow row, int rowIndex) {
        int totalWidth = 0;
        if (columnWidths != null) {
            for (int w : columnWidths) {
                totalWidth += w;
            }
        }
        RowBlueprint.applyBounds(blueprintPanel, totalWidth, getHeight());

        if (bindScript != null && interpreter != null) {
            try {
                interpreter.set("row",      row);
                interpreter.set("rowIndex", rowIndex);
                interpreter.eval(bindScript);
            } catch (bsh.EvalError e) {
                System.err.println("ScriptableRecyclable bind error [row " + rowIndex + "]: " + e.getMessage());
            }
        }
    }

    @Override
    public void prepareForReuse() {
        if (prepareScript != null && interpreter != null) {
            try {
                interpreter.eval(prepareScript);
            } catch (bsh.EvalError e) {
                System.err.println("ScriptableRecyclable prepare error: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------

    private void initInterpreter() {
        try {
            interpreter = new bsh.Interpreter();
            interpreter.eval("import org.jwellman.swing.grid.*;");
            interpreter.eval("import javax.swing.*;");
            interpreter.eval("import java.awt.*;");
            interpreter.set("self",  this);           // the outer ScriptableRecyclable (for background etc.)
            interpreter.set("panel", blueprintPanel); // the inner blueprint JPanel (for ComponentFinder)
        } catch (bsh.EvalError e) {
            System.err.println("ScriptableRecyclable: interpreter init failed: " + e.getMessage());
            interpreter = null;
        }
    }
}
