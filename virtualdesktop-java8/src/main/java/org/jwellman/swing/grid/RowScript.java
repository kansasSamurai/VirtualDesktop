package org.jwellman.swing.grid;

/**
 * BeanShell-implementable interface for scripted row rendering.
 *
 * Implement this in the BeanShell console and pass it to
 * {@link DefaultGridComponentFactory#create(String, String, RowScript, ScriptBridge)}.
 * The methods are called directly (no string eval) so full BeanShell editing
 * and console-namespace closures are available.
 *
 * <pre>
 * RowScript r = new RowScript() {
 *     public void bind(ScriptableRecyclable self, GridRow row, int rowIndex) {
 *         self.setBackground(new Color(0x8B0000));
 *         ((JLabel) self.find("ts")).setText(row.get("timestamp").toString());
 *     }
 *     public void reset(ScriptableRecyclable self) {
 *         self.setBackground(null);
 *     }
 * };
 * gridFactory.create("delete-event", layout, r, bridge);
 * </pre>
 */
public interface RowScript {

    /**
     * Called on each bind cycle to populate the row's components from {@code row}.
     *
     * @param self     the {@link ScriptableRecyclable} being rendered; use
     *                 {@code self.find("id")} to locate child components and
     *                 {@code self.setBackground(...)} to color the row
     * @param row      the data row being bound
     * @param rowIndex the absolute row index (for alternating-color logic etc.)
     */
    void bind(ScriptableRecyclable self, GridRow row, int rowIndex);

    /**
     * Called before the component is returned to the pool.
     * Clear text, reset colors, remove any listeners added in {@code bind}.
     */
    void reset(ScriptableRecyclable self);
}
