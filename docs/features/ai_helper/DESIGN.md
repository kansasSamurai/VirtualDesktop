# Design Document

## Introduction

*Claude Design: `AI Project Workspace` sessionid: cse_01BVbok4Fwyk19cT8kde2YKr*

This is a trail of design discussions and decisions using claude design.  
It is organized by date, most recent first.

## Initial Design 11/25/2026

I've built the AI Project Workspace breakdown on your canvas as four artboards:

- **[Main window](https://claude.ai/artifact/5exdWMPi2UT3QU7Z1MPWZR#artboard-Main):** an editable copy of the Project Posture screen. Its parts are real buttons, a search field and lists rather than a flat picture.
- **[Swing region map](https://claude.ai/artifact/5exdWMPi2UT3QU7Z1MPWZR#artboard-Regions):** the screen as a tree of Swing containers. The frame uses BorderLayout. The toolbar sits in the title bar, a split pane holds the project list on the left (divider at 336 px), and the right side uses MigLayout. The status bar runs along the bottom.
- **[Tokens](https://claude.ai/artifact/5exdWMPi2UT3QU7Z1MPWZR#artboard-Tokens):** the colors, the four status colors (OK, Warn, Drift, Waived), type sizes, spacing and corner radii taken from the screenshot. Each one is paired with the FlatLaf or UIManager setting it maps to.
- **[Components](https://claude.ai/artifact/5exdWMPi2UT3QU7Z1MPWZR#artboard-Components):** each reusable piece with its states and the Java class that will own it. That covers the status badge, project list row, vendor card, health panel with its check rows, and the buttons, filters and search field.

I made a few changes from the screenshot that you should check:

- **Vendor logos:** the vendor cards show two-letter placeholders (Cl, Cx, Cu, Ge) instead of the real logos. Each vendor's approved logo can go in that 22×22 icon spot when you build it.
- **Pass checks:** the screenshot used both "[v]" and ✓ for a passed check, so I used ✓ everywhere.
- **Status bar:** the screenshot showed the same counts twice. Now the counts are on the left and only "Last Scan" is on the right.
- **Fonts:** I picked Inter for the interface and JetBrains Mono for paths and badges. FlatLaf ships both as add-on font packages, so they'll look the same in Swing.

Everything is in this canvas for now. If you'd like it as a standalone design system you can reuse in other projects, like your Horizon one, I can make that next.
