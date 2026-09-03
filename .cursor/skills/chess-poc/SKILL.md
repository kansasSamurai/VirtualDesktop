---
name: chess-poc
description: >-
  Guides work on the chess proof-of-concept in org.jwellman.demo.chess
  (ChessUiEngine and friends) — a standalone Swing chess board with
  drag-and-drop, undo/redo, and a custom LayoutManager2. Use when the user
  mentions ChessUiEngine, the chess demo/PoC, chess board rendering, flip
  board / board orientation, or chess piece drag-and-drop in this repo.
---

# Chess PoC (`org.jwellman.demo.chess`)

Located at `virtualdesktop-java8/src/main/java/org/jwellman/demo/chess/`. A **standalone Swing demo** — `ChessUiEngine.main()` builds its own `JFrame` directly. It does **not** participate in the VirtualDesktop `vapps`/`VirtualAppFrame` framework and does **not** use the `appstage` toolkit (see the [[appstage]] skill for that, unrelated pattern) — its "Control Panel" and "Settings" are plain `TitledBorder`-wrapped `JPanel`s in the east sidebar, not a menu system or a stage.

## When to use

- Adding/editing chess rules, move validation, or game state (`ChessGame`, `ChessMoveValidator`)
- Changing how the board renders, adding a new Settings toggle, or touching board orientation
- Debugging drag-and-drop, undo/redo, or promotion flow
- Extending the piece/token visuals or the gutter (captured-piece banks, turn indicator)

## Core architecture: model/view split

- **Model** (orientation-independent, canonical truth): `ChessGame.activePieces` is a `Map<Point, ChessPiece>` keyed by **logical** `(file, rank)`, both `0–7` (`a1` = `(0,0)`, `h8` = `(7,7)`). `ChessPiece.position` mirrors this. Move validation (`ChessMoveValidator`, `SquareControlMatrix`) operates purely in this logical space — it has no idea which side is drawn at the bottom.
- **View mirror**, kept in sync by `ChessUiEngine`: a static `BoardSquare[8][8] boardSquareMatrix` (indexed `[file][rank]`) and a `Map<ChessPiece, ChessPieceToken> viewTokens`. `addPieceToBoard()` is the single choke point that re-syncs a piece's view token to its model position (used by both drag-drop and undo/redo).
- `mousePressed` contains a deliberate desync detector ("FORENSIC ALERT") comparing the token's domain position vs. the layout manager's cached coordinate — this is intentional diagnostics, not dead code; don't remove it when refactoring nearby.

**Rule of thumb when extending:** anything that reasons about *where a piece can move* belongs in `ChessGame`/`ChessMoveValidator` and stays in logical coordinates. Anything that reasons about *pixels on screen* belongs in `ChessUiEngine` (or `ChessGutterBorder`) and must go through the coordinate transform below — never hardcode a `7 - x` inversion inline.

## Rendering pipeline (two independent systems)

1. **Pieces**: `ChessBoardLayout` (a custom `LayoutManager2`, static nested class in `ChessUiEngine`) maps each piece token's logical coordinate to pixel bounds in `layoutContainer()`.
2. **Checkerboard background**: a separate `JPanel(GridLayout(8,8))` (`boardBackground`) whose *child insertion order* determines visual position — not `ChessBoardLayout`. Reordering it means `removeAll()` + re-add in the desired order (see `rebuildBoardBackgroundOrder`), not recreating `BoardSquare` instances (their color/rank/file are fixed at construction and don't need to change).

Both systems, plus the inverse transform in `PieceFlightController.mouseReleased` (pixel → logical, on drop) and `ChessGutterBorder`'s turn-indicator placement, all encode the same **orientation transform**. As of the board-flip feature, orientation is a `flipped` boolean threaded through all four spots (`ChessBoardLayout.flipped`, read via `layout.isFlipped()` in the mouse controller, `ChessGutterBorder.flipped`, and `ChessUiEngine.setFlipped()` driving `rebuildBoardBackgroundOrder`):

```java
int screenCol = flipped ? (7 - file) : file;   // visual left→right
int screenRow = flipped ? rank       : (7 - rank); // visual top→bottom
```

`Options.Chooser.FLIP_BOARD` (Settings panel) drives `ChessUiEngine.setFlipped(boolean)`, which is the single entry point that updates all four spots and repaints. If you add another orientation-sensitive visual, wire it through `setFlipped()` too rather than reading a flag ad hoc.

## Drag-and-drop flow

Only drag-and-drop is implemented (no click-to-move). `ChessUiEngine.PieceFlightController` is a single shared `MouseAdapter` attached to every `ChessPieceToken`:

- `mousePressed` — turn check, computes valid destinations/captures via `game.getValidMovementSquares`/`getValidControlSquares`, marks target `BoardSquare`s via the flip-safe `boardSquareMatrix[file][rank]` lookup, lifts the piece to `JLayeredPane.DRAG_LAYER`.
- `mouseDragged` — pure pixel delta, no board-coordinate math.
- `mouseReleased` — pixel center → logical coordinate (orientation-aware, see above), checks promotion (`game.isPromotionPending` → `PromotionChoiceDialog`), else `game.submitMove(...)`; records a `MoveEvent` onto `undoStack` on success.

Undo/redo (`ChessUiEngine.undoLastMove`) replays `MoveEvent`s through `game.restoreMovedPiece`/`restoreCapturedPiece` and re-syncs views via `addPieceToBoard`.

## Settings pattern

`Options.java` holds one `JCheckBox` per toggle inside a nested `Chooser` class, plus a boolean getter (e.g. `showSquareStrength()`, `isFlipped()`). `ChessUiEngine.createSettings()` adds the checkbox to a `Box.createVerticalBox()` and wires an `ActionListener`. Follow this exact pattern for new toggles — don't introduce a menu or a different settings mechanism.

## Checklist for changes here

- [ ] Java 8 only; repo formatting conventions (braces always, one statement per line — see root `CLAUDE.md`)
- [ ] New move/rule logic goes in `ChessGame`/`ChessMoveValidator`, stays in logical `(file, rank)` space
- [ ] New screen-facing math goes through the shared orientation transform, not an inline `7 - x`
- [ ] New Settings toggle follows the `Options.Chooser` + `createSettings()` pattern
- [ ] Don't wire this into `VirtualAppFrame`/`appstage` unless explicitly asked — it's meant to stay a standalone demo for now

## More detail

File-by-file map and line counts: [reference.md](reference.md)
