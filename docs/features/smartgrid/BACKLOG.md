# Ideas to incorporate into the roadmap

## keyboard navigation


Moving toward keyboard navigation is the perfect logical step to turn this from a passive display into an active tool.

Baking the core keyboard navigation directly into your underlying `SmartGrid` component is absolutely the right architectural move. It keeps the calendar code decoupled from raw input handling and ensures that any other grid-based tool you build in your desktop framework gets keyboard support for free.

Here is a blueprint for implementing this in a clean, fail-fast `SmartGrid` architecture.

---

## 1. The Strategy: Focal Cell Tracking

To implement this seamlessly in a component-based grid, the `SmartGrid` needs to maintain a stateful concept of the **Active/Focused Cell** using grid coordinates (row and column index).

Instead of relying on Swing's standard focus manager to jump between sub-components (which can cause focus loops or weird tab behaviors), the `SmartGrid` itself should capture the directional key strokes, update its internal coordinate tracking, and request a repaint.

---

## 2. SmartGrid Key Bindings Blueprint

Using Swing's `InputMap` and `ActionMap` is much cleaner than a `KeyListener` because it handles key repeats correctly and won't conflict with child component inputs.

Here is a structural example of how you can map directional navigation right into the grid:

```java
import javax.swing.*;
import java.awt.event.ActionEvent;

public class SmartGrid extends JPanel {
    private int selectedRow = 0;
    private int selectedCol = 0;
    
    private int totalRows;
    private int totalCols;

    public SmartGrid(int rows, int cols) {
        this.totalRows = rows;
        this.totalCols = cols;
        this.setFocusable(true);
        
        setupKeyboardNavigation();
    }

    private void setupKeyboardNavigation() {
        InputMap im = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = getActionMap();

        // Map keystrokes to action keys
        im.put(KeyStroke.getKeyStroke("LEFT"), "moveLeft");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "moveRight");
        im.put(KeyStroke.getKeyStroke("UP"), "moveUp");
        im.put(KeyStroke.getKeyStroke("DOWN"), "moveDown");
        im.put(KeyStroke.getKeyStroke("ENTER"), "triggerCell");

        // Bind action keys to explicit logic
        am.put("moveLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { moveSelection(0, -1); }
        });
        am.put("moveRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { moveSelection(0, 1); }
        });
        am.put("moveUp", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { moveSelection(-1, 0); }
        });
        am.put("moveDown", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { moveSelection(1, 0); }
        });
        am.put("triggerCell", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { fireCellAction(selectedRow, selectedCol); }
        });
    }

    private void moveSelection(int rowDelta, int colDelta) {
        // Calculate new bounds with defensive boundaries (fail-safe layout constraints)
        int newRow = selectedRow + rowDelta;
        int newCol = selectedCol + colDelta;

        if (newRow >= 0 && newRow < totalRows && newCol >= 0 && newCol < totalCols) {
            this.selectedRow = newRow;
            this.selectedCol = newCol;
            
            // Notify layout/renderer to draw the focus border around the new cell
            repaint(); 
            notifySelectionListeners();
        }
    }
    
    private void fireCellAction(int row, int col) {
        // This is where you pass execution upward to launch your "Add Event" dialog
        System.out.println("Cell activated at: " + row + ", " + col);
    }
}

```

---

## 3. Bridging the Grid to the Calendar Application

Once `SmartGrid` knows which cell has focus, your calendar layout engine can listen for those selection changes or actions to enable data entry:

* **The Selection Border:** In your cell renderer, check if the cell's date matches the current `selectedRow` and `selectedCol`. If it does, paint a crisp 1px dashed or solid accent border around that specific day to show it's selected.
* **The "Enter" Trigger:** When `fireCellAction` is triggered on a day, instead of hardcoding a dummy event, your controller can launch a compact, borderless modal popup or internal frame at the cell's coordinates. This popup would feature a simple `JTextField` to instantly capture the event title (e.g., typing "Team Sync" and hitting Enter inserts a new green block into that day's list).

By standardizing this in `SmartGrid`, you solve navigation for the calendar while instantly laying down the infrastructure for any future data density grids you want to build into your orchestrator.

Does this coordinate-mapping strategy match how you've structured the underlying grid renderer so far?