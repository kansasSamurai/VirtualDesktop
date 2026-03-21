# doxygen

## Summary

put somethere here

1. Create the Doxyfile

2. Run the Analysis

    Install Doxygen & Graphviz (ensure dot is in your PATH).

    Open a terminal in your project root.

    Type: doxygen Doxyfile

    Open ./docs/doxygen/html/index.html in your browser.

3. How to find your "DAO-to-Controller" Path

    Once the HTML is generated:

    Search for your DAO method name in the top-right search bar.

    Click the method name to go to its documentation page.

    Scroll down to the "Caller graph for this function" section.

    You will see a visual map showing every method that calls that DAO. You can click the nodes to navigate "upward" until you see your @RestController methods.

### VS Code config

```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Generate Docs",
            "type": "shell",
            "command": "doxygen Doxyfile",
            "group": {
                "kind": "build",
                "isDefault": true
            },
            "problemMatcher": [],
            "presentation": {
                "reveal": "silent"
            }
        }
    ]
}
```

### Project Doxyfile

This config is optimized for a modern, clean sidebar experience.
Replace the relevant tags in your Doxyfile with these:

```ini
# Project Metadata
PROJECT_NAME           = "Project Name"
OUTPUT_DIRECTORY       = ./docs/doxygen
OPTIMIZE_OUTPUT_JAVA   = YES
EXTRACT_ALL            = YES
EXTRACT_PRIVATE        = YES
EXTRACT_STATIC         = YES

# Input Settings (Point to your Java source)
INPUT                  = ./src/main/java
RECURSIVE              = YES
FILE_PATTERNS          = *.java

# Diagram Settings (The "Forensic" Magic)
HAVE_DOT               = YES
DOT_PATH               = "C:/Program Files/Graphviz/bin" # Adjust for your OS
DOT_IMAGE_FORMAT       = svg
INTERACTIVE_SVG        = YES
GENERATE_GRAPHVIZ      = YES

# This generates the "Who calls this DAO?" maps
CALL_GRAPH             = YES
CALLER_GRAPH           = YES
DIRECTORY_GRAPH        = YES
GRAPHICAL_HIERARCHY    = YES

# Output Format
GENERATE_HTML          = YES
GENERATE_LATEX         = NO
HTML_OUTPUT            = html

# --- Modern Layout Configuration ---
GENERATE_TREEVIEW      = YES
DISABLE_INDEX          = NO
FULL_SIDEBAR           = NO

# Point this to where you placed the 'doxygen-awesome.css' file
HTML_EXTRA_STYLESHEET  = docs/doxygen-awesome.css # Use a global/static location

# Make sure colors are light-mode compatible if using the default Awesome theme
HTML_COLORSTYLE        = LIGHT

```

## Optimizations

That 20–30 minute sync for just one directory is exactly why most developers abandon Doxygen before they see the good stuff. In a 40-year-old codebase, you aren't just parsing text; you are asking `dot` to perform complex math to arrange thousands of nodes.

Here is how to tune Doxygen to run in **minutes instead of lunch breaks**.

### 1. The "Turbo" Settings (Immediate Impact)

Add these to your config file to use all your CPU cores and skip unnecessary work.

```ini
# Use all your CPU cores for graph generation (0 = auto-detect)
DOT_NUM_THREADS        = 0 

# Multi-threaded parsing (Available in newer Doxygen versions)
NUM_PROC_THREADS       = 0

# Skip redundant checks
LOOKUP_CACHE_SIZE      = 2 # Increase if you have 16GB+ RAM
```

### 2. The "Graph Blast" Limiters

This is likely where your 30 minutes are going. By default, Doxygen tries to map *everything*. In a legacy system, one "Utility" class can cause a "nuclear explosion" of arrows.

```ini
# Stop the "Spaghetti" effect
MAX_DOT_GRAPH_DEPTH    = 3   # Only show 3 levels of calls (plenty for a DAO trace)
DOT_GRAPH_MAX_NODES    = 50  # If a graph needs >50 nodes, it's unreadable anyway

# Disable graphs you don't need for forensics
INCLUDE_GRAPH          = NO
INCLUDED_BY_GRAPH      = NO
DIRECTORY_GRAPH        = NO
COLLABORATION_GRAPH    = NO
```

### 3. Use "Incremental" Mode (The Pro Secret)

Doxygen has a "smart" mode where it doesn't redraw a graph if the code hasn't changed.

* **How to enable it:** Set `DOT_CLEANUP = NO`.
* **The Benefit:** Your first run will still take 20 minutes, but your second run (after changing one Service) will take **seconds**.
* **Note:** Since you added `.docs` to your `.gitignore`, this cache stays on your machine and won't bloat the repo.

### 4. Switch to SVG (If not already)

If you are generating PNGs, `dot` has to rasterize every pixel.

* **Set:** `DOT_IMAGE_FORMAT = svg`
* **Set:** `INTERACTIVE_SVG = YES`
* **Why:** SVGs are just XML text; they generate faster, look sharper, and (most importantly) allow you to **Ctrl+F** search for a method name *inside* the diagram.

---

### My Recommendation for your "Large" Project

Don't run the full analysis every time. Create a **"Fast Trace"** config file:

1. Copy your main config to `trace.doxconfig`.
2. Set `INPUT = ./src/main/java/com/your-target-package`.
3. Set `RECURSIVE = NO` (or a very shallow depth).
4. Run this when you're actively hunting a specific bug. Save the "Full Build" for an overnight task or a long coffee break.
