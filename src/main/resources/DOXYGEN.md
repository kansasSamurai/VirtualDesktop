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

You are exactly right—Doxygen is a "global" analyzer. To draw a call graph from a Controller to a DAO, it has to have parsed both ends of the string. If you only point it at the `Service` layer, it will show the calls *leaving* the service, but it won't know those calls land in your `DAO` unless the DAO is also in the `INPUT`.

However, for a 40-year-old codebase, "letting it loose" overnight is a classic rite of passage. Here is the incremental "Senior Dev Strategy" to make that overnight run actually useful without it crashing at 3:00 AM.

### 1. The "Two-Pass" Strategy

Instead of one massive build, create two separate `.doxconfig` files:

* **`Architecture_Map.doxconfig` (The Light Run):**
  * `HAVE_DOT = NO`
  * **Goal:** This will finish in minutes. It builds the text-based hierarchy and search index. Use this to verify that Doxygen can actually "see" all your source folders and that your `EXCLUDE_PATTERNS` are working. If this fails, the heavy run will definitely fail.
* **`Forensic_DeepDive.doxconfig` (The Overnight Run):**
  * `HAVE_DOT = YES`
  * `CALL_GRAPH = YES`
  * `CALLER_GRAPH = YES`
  * **Goal:** This is the one you "let loose."

### 2. The "Safety Valves" for the Overnight Run

To ensure you don't wake up to a hung process or a 50GB folder of nonsense, add these constraints:

```ini
# Limit the "Spaghetti" factor
DOT_GRAPH_MAX_NODES    = 100  # Prevents infinite-looking "God Class" graphs
MAX_DOT_GRAPH_DEPTH    = 4    # Usually enough to see Controller -> Service -> DAO -> DB
UML_LOOK               = YES  # Makes the graphs slightly more compact/readable

# Prevent "Path Too Long" errors on Windows
SHORT_NAMES            = YES 

# Don't let it hang on a single broken file
WARN_AS_ERROR          = NO
```

### 3. Use the "External Tag File" Approach (Advanced Incremental)

If you truly want to work incrementally, Doxygen has a feature called **Tag Files**.

1. You run Doxygen on your `Foundation` framework and generate a `foundation.tag` file.

2. When you run Doxygen on your *work* project, you tell it to consume that tag file (`TAGFILES = foundation.tag=../../foundation/docs/html`).

3. **The Result:** Doxygen won't re-parse the framework code, but it will still be able to create clickable links from your work code into the framework's documentation.

### My Recommendation

I'd say **let it loose**, but with a "Wrapper" script to time it and log the results. Create a simple `.bat` file:

```batch
@echo off
echo Starting Forensic Build at %TIME%
doxygen Forensic_DeepDive.doxconfig > build_log.txt 2>&1
echo Build Finished at %TIME%
pause
```

This way, tomorrow morning, you’ll know exactly how long it took (the "timing feel" you wanted) and you can check the `build_log.txt` for any "Patching" errors.
