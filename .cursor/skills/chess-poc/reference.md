# Chess PoC — reference

Package: `org.jwellman.demo.chess`
Path: `virtualdesktop-java8/src/main/java/org/jwellman/demo/chess/`
Entry point: `ChessUiEngine.main()` (standalone `JFrame`, not a registered vapp)

## File map

| File | Lines | Role |
|---|---:|---|
| `ChessUiEngine.java` | ~673 | Main class: Swing wiring, `ChessBoardLayout` (custom `LayoutManager2`), `PieceFlightController` (drag/drop mouse handler), Control Panel/Settings panels, undo/redo, board-flip orchestration |
| `ChessGame.java` | 423 | Domain model / game state — authoritative. `Map<Point, ChessPiece> activePieces`, turn tracking, en passant, promotion, `submitMove`, `restoreMovedPiece`/`restoreCapturedPiece` for undo |
| `BoardSquare.java` | 284 | One square's `JPanel` — background color, targeted/highlighted painting, square-strength heatmap overlay. Constructed once with fixed `(rank, file)`; never recreated on flip, only repositioned |
| `ChessGutterBorder.java` | 174 | Custom `EmptyBorder` painting the 40px perimeter gutter: background wash, turn-indicator pill (`drawNewPlayerIndicator`, orientation-aware via `flipped`), placeholder captured-piece bank slots (decorative, not orientation-aware — no real per-color data feeds them) |
| `ChessPiece.java` | 197 | Domain piece: type, color, `position` (logical `Point`), movement vectors |
| `ChessMoveValidator.java` | 129 | Raycasting move/attack-square generator, logical-coordinate only |
| `ChessPieceToken.java` | 105 | `JToggleButton`-based piece view widget (circle + glyph); orientation-agnostic rendering (no directional shape) |
| `MoveEvent.java` | 71 | Immutable move record (undo/redo, algebraic notation) |
| `MovesScoresheetPanel.java` | 84 | `JTable`-based move list view, driven by `synchronizeHistory(undoStack)` |
| `MoveAnalysis.java` | 68 | Move-submission result value object (`isAccepted()`, `getCapturedPiece()`, `getResultType()`) |
| `ChessMoveVectors.java` | 37 | Static direction-vector constants |
| `SquareControlMatrix.java` | 35 | `int[8][8]` attacker-count heatmap, keyed `[file][rank]` |
| `PromotionChoiceDialog.java` | 45 | Modal promotion picker |
| `Options.java` | 30 | `Chooser` inner class holding one `JCheckBox` per Settings toggle (`SHOW_SQUARE_STRENGTH`, `FLIP_BOARD`) plus boolean getters |
| `CollisionMap.java` | 28 | Interface abstraction over board occupancy |
| `RaycastObserver.java` | 17 | Callback interface used by the validator |

No coordinate labels (a–h / 1–8) are drawn anywhere in the app.

## Coordinate system

Logical space: `Point(file, rank)`, both `0–7`. `file 0` = a-file, `rank 0` = 1st rank. `a1 = (0,0)`, `h8 = (7,7)`. This is the *only* space the model (`ChessGame`, `ChessPiece`, `ChessMoveValidator`, `SquareControlMatrix`) knows about.

Screen space transform (in `ChessUiEngine.ChessBoardLayout.layoutContainer`, mirrored inversely in `PieceFlightController.mouseReleased`):

```java
int screenCol = flipped ? (7 - file) : file;
int screenRow = flipped ? rank       : (7 - rank);
int x = screenCol * squareSize + BOARD_BORDER_SIZE;
int y = screenRow * squareSize + BOARD_BORDER_SIZE;
```

Non-flipped: white at the bottom, a-file on the left (standard). Flipped: 180° rotation — black at the bottom, h-file on the left. `boardBackground`'s `GridLayout(8,8)` child insertion order must match the same visual ordering; see `ChessUiEngine.rebuildBoardBackgroundOrder(boolean)`.

## Key entry points

| Method | File | Purpose |
|---|---|---|
| `ChessUiEngine.setFlipped(boolean)` | ChessUiEngine.java | Single choke point for board orientation — updates `layout`, `gutterBorder`, rebuilds `boardBackground` order, repaints |
| `ChessUiEngine.addPieceToBoard(ChessPiece, boolean)` | ChessUiEngine.java | Re-syncs a piece's view token (`viewTokens`) to its model position — used by drop, promotion, and undo/redo |
| `ChessGame.submitMove(Point from, Point to)` | ChessGame.java | Applies a move to the model; returns `MoveAnalysis` |
| `ChessUiEngine.recordMove` / `undoLastMove` | ChessUiEngine.java | `undoStack`/`redoStack` of `MoveEvent` |
| `PieceFlightController` (inner class) | ChessUiEngine.java | Shared `MouseAdapter` for all `ChessPieceToken`s — see `mousePressed`/`mouseDragged`/`mouseReleased` |

## Static/shared state (watch for these when refactoring)

- `ChessUiEngine.boardSquareMatrix` — `static final BoardSquare[8][8]`, indexed `[file][rank]`, logical-coordinate lookup, flip-safe by construction
- `ChessUiEngine.viewTokens` — `static final Map<ChessPiece, ChessPieceToken>`
- `BoardSquare.drawControlIndicators` — `static boolean` toggled by the "Show Square Strength" checkbox

Both `boardSquareMatrix` and `viewTokens` are `static`, which is a code smell for anything beyond a single-window PoC (a second `ChessUiEngine` instance would collide) — flagged here, not something to silently "fix" unless asked; it hasn't mattered so far since only one board is ever instantiated.

## Known intentional oddities (don't "clean up" without asking)

- `mousePressed`'s "FORENSIC ALERT" and `mouseReleased`'s "DIAGNOSTIC" console prints are deliberate desync detectors comparing view state vs. domain state — not debug leftovers.
- The anonymous `JLayeredPane` `paintComponent` override in the `ChessUiEngine` constructor (draws its own checkerboard) is effectively dead — `boardBackground` visually covers it. Left alone; not part of the flip-sensitive rendering path.
