package org.jwellman.swing.grid;

import java.awt.Component;
import javax.swing.JPanel;

/**
 * A row component whose bind/reset lifecycle is driven by BeanShell scripts.
 *
 * The component tree inside the panel is built from an XML blueprint by
 * {@link RowBlueprint}. Scripts execute in either a dedicated per-instance
 * interpreter or an injected shared interpreter (e.g. the VirtualDesktop
 * console's {@code _interpreter}).
 *
 * Scripts receive these variables on every call:
 *   self     — this ScriptableRecyclable; call self.find("id") to reach children,
 *              or self.setBackground(Color.RED) to color the row
 *   panel    — the inner blueprint JPanel
 *   row      — the GridRow being rendered (bind script only)
 *   rowIndex — the row index (bind script only)
 *
 * The shared-interpreter path is safe because all Swing operations run on the
 * EDT serially — there is no concurrent access to the interpreter's variables.
 *
 * <p>Preferred console-friendly constructor:
 * <pre>
 *   String layout = "&lt;row&gt;&lt;label id='ts'/&gt;&lt;label id='user'/&gt;&lt;/row&gt;";
 *   String bind   = "self.find('ts').setText(row.get('timestamp'));";
 *   String reset  = "self.setBackground(null);";
 *   ScriptBridge bridge = new ScriptBridge(grid, _interpreter);
 *   new ScriptableRecyclable(layout, bind, reset, bridge)
 * </pre>
 */
public class ScriptableRecyclable extends JPanel implements Recyclable {

    private final JPanel          blueprintPanel;
    private final int[]           columnWidths;
    private       String          bindScript;
    private       String          prepareScript;
    private       bsh.Interpreter interpreter;

    // -------------------------------------------------------------------------
    // Console-friendly constructor: layout + scripts as separate strings
    // -------------------------------------------------------------------------

    /**
     * Parses {@code layoutXml} into a component tree and stores the bind/reset
     * scripts for execution via the {@code bridge}'s interpreter.
     *
     * <p>Flat XML format (no &lt;column&gt; wrappers needed):
     * {@code <row><label id='ts'/><label id='user'/><button id='act' text='Undo'/></row>}
     */
    public ScriptableRecyclable(String layoutXml, String bindScript,
                                String resetScript, ScriptBridge bridge) {
        JPanel panel;
        try {
            panel = RowBlueprint.buildPanel(layoutXml);
        } catch (Exception e) {
            System.err.println("ScriptableRecyclable: layout parse failed: " + e.getMessage());
            panel = new JPanel();
        }
        this.blueprintPanel = panel;
        this.columnWidths   = bridge.getColumnWidths();
        this.interpreter    = bridge.getInterpreter();
        this.bindScript     = bindScript;
        this.prepareScript  = resetScript;
        setLayout(null);
        setOpaque(true);
        add(blueprintPanel);
    }

    // -------------------------------------------------------------------------
    // Programmatic constructors (used by DefaultGridComponentFactory.create())
    // -------------------------------------------------------------------------

    /** Uses a dedicated per-instance interpreter (standalone / demo mode). */
    public ScriptableRecyclable(JPanel blueprintPanel, int[] columnWidths) {
        this(blueprintPanel, columnWidths, null);
    }

    /**
     * Uses the supplied interpreter when non-null (shared desktop context),
     * or falls back to a dedicated per-instance interpreter when null.
     */
    public ScriptableRecyclable(JPanel blueprintPanel, int[] columnWidths,
                                bsh.Interpreter sharedBsh) {
        this.blueprintPanel = blueprintPanel;
        this.columnWidths   = columnWidths;
        setLayout(null);
        setOpaque(true);
        add(blueprintPanel);
        if (sharedBsh != null) {
            this.interpreter = sharedBsh;
        } else {
            initPrivateInterpreter();
        }
    }

    // -------------------------------------------------------------------------

    public void setBindScript(String script) {
        this.bindScript = script;
    }

    public void setPrepareScript(String script) {
        this.prepareScript = script;
    }

    /**
     * Finds a named component anywhere within this row's component hierarchy.
     * Intended for use in BeanShell bind scripts: {@code self.find("labelId")}.
     */
    public Component find(String id) {
        return ComponentFinder.find(this, id);
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
                interpreter.set("self",     this);
                interpreter.set("panel",    blueprintPanel);
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
                interpreter.set("self",  this);
                interpreter.set("panel", blueprintPanel);
                interpreter.eval(prepareScript);
            } catch (bsh.EvalError e) {
                System.err.println("ScriptableRecyclable prepare error: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------

    private void initPrivateInterpreter() {
        try {
            interpreter = new bsh.Interpreter();
            interpreter.eval("import org.jwellman.swing.grid.*;");
            interpreter.eval("import javax.swing.*;");
            interpreter.eval("import java.awt.*;");
        } catch (bsh.EvalError e) {
            System.err.println("ScriptableRecyclable: interpreter init failed: " + e.getMessage());
            interpreter = null;
        }
    }
}
