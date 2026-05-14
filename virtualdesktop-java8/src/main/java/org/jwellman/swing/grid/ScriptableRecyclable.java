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
 * All instances for a given fnd-type share a single {@link ScriptSpec}.
 * Updating the spec (via {@link DefaultGridComponentFactory#create} called
 * again with the same key) takes effect on every live instance on the next
 * {@code bind()} call — no pool replacement or slot recycling required.
 *
 * Scripts receive these variables on every call:
 *   self     — this ScriptableRecyclable; use self.find("id") to reach children
 *              or self.setBackground(Color.RED) to color the row
 *   panel    — the inner blueprint JPanel
 *   row      — the GridRow being rendered (bind script only)
 *   rowIndex — the row index (bind script only)
 *
 * <p>Preferred console-friendly usage via factory (enables live swap):
 * <pre>
 *   ScriptBridge bridge = new ScriptBridge(grid, _interpreter);
 *   gridFactory.create("myType", layout, bind, reset, bridge);
 *   // later, to swap live:
 *   gridFactory.create("myType", layout, newBind, reset, bridge);
 *   model.notifyDataChanged();
 * </pre>
 */
public class ScriptableRecyclable extends JPanel implements Recyclable {

    private final JPanel          blueprintPanel;
    private final int[]           columnWidths;
    private final ScriptSpec      scriptSpec;
    private       bsh.Interpreter interpreter;

    // -------------------------------------------------------------------------
    // Factory-managed constructor — ScriptSpec is shared across all pool instances
    // -------------------------------------------------------------------------

    /** Called by DefaultGridComponentFactory; shares the spec across the pool. */
    ScriptableRecyclable(JPanel blueprintPanel, int[] columnWidths,
                         bsh.Interpreter interpreter, ScriptSpec scriptSpec) {
        this.blueprintPanel = blueprintPanel;
        this.columnWidths   = columnWidths;
        this.interpreter    = interpreter;
        this.scriptSpec     = scriptSpec;
        setLayout(null);
        setOpaque(true);
        add(blueprintPanel);
        if (interpreter == null) {
            initPrivateInterpreter();
        }
    }

    // -------------------------------------------------------------------------
    // Console-friendly constructor — layout + scripts as separate strings
    // -------------------------------------------------------------------------

    /**
     * Parses {@code layoutXml} into a component tree and stores the bind/reset
     * scripts for execution via the {@code bridge}'s interpreter.
     *
     * <p>Flat XML format (no &lt;column&gt; wrappers needed):
     * {@code <row><label id='ts'/><label id='user'/><button id='act' text='Undo'/></row>}
     *
     * <p>Note: instances created directly via this constructor each own their
     * own {@link ScriptSpec}. For live script swapping, use
     * {@link DefaultGridComponentFactory#create(String, String, String, String, ScriptBridge)}
     * instead so all pool instances share one spec.
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
        this.scriptSpec     = new ScriptSpec(bindScript, resetScript);
        setLayout(null);
        setOpaque(true);
        add(blueprintPanel);
    }

    // -------------------------------------------------------------------------
    // Programmatic constructors (used internally by DefaultGridComponentFactory)
    // -------------------------------------------------------------------------

    /** Uses a dedicated per-instance interpreter (standalone / demo mode). */
    public ScriptableRecyclable(JPanel blueprintPanel, int[] columnWidths) {
        this(blueprintPanel, columnWidths, (bsh.Interpreter) null);
    }

    /**
     * Uses the supplied interpreter when non-null (shared desktop context),
     * or falls back to a dedicated per-instance interpreter when null.
     */
    public ScriptableRecyclable(JPanel blueprintPanel, int[] columnWidths,
                                bsh.Interpreter sharedBsh) {
        this.blueprintPanel = blueprintPanel;
        this.columnWidths   = columnWidths;
        this.scriptSpec     = new ScriptSpec(null, null);
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
        scriptSpec.bindScript = script;
    }

    public void setPrepareScript(String script) {
        scriptSpec.resetScript = script;
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

        if (scriptSpec.adapter != null) {
            // Adapter path: direct method dispatch — no string eval overhead
            try {
                scriptSpec.adapter.bind(this, row, rowIndex);
            } catch (Exception e) {
                System.err.println("ScriptableRecyclable bind error [row " + rowIndex + "]: " + e.getMessage());
            }
        } else {
            // String path: eval bind script in the interpreter
            String bs = scriptSpec.bindScript;
            if (bs != null && interpreter != null) {
                try {
                    interpreter.set("self",     this);
                    interpreter.set("panel",    blueprintPanel);
                    interpreter.set("row",      row);
                    interpreter.set("rowIndex", rowIndex);
                    interpreter.eval(bs);
                } catch (bsh.EvalError e) {
                    System.err.println("ScriptableRecyclable bind error [row " + rowIndex + "]: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void prepareForReuse() {
        if (scriptSpec.adapter != null) {
            // Adapter path
            try {
                scriptSpec.adapter.reset(this);
            } catch (Exception e) {
                System.err.println("ScriptableRecyclable prepare error: " + e.getMessage());
            }
        } else {
            // String path
            String rs = scriptSpec.resetScript;
            if (rs != null && interpreter != null) {
                try {
                    interpreter.set("self",  this);
                    interpreter.set("panel", blueprintPanel);
                    interpreter.eval(rs);
                } catch (bsh.EvalError e) {
                    System.err.println("ScriptableRecyclable prepare error: " + e.getMessage());
                }
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
