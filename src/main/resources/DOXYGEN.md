# Doxygen

## Summary

Doxygen is essentially a "static analysis engine" that masquerades as a documentation tool. It doesn't just read comments; it builds an internal map (a parse tree) of your entire codebase—classes, methods, fields, and relationships—and then uses that map to generate human-readable documentation.

Because it understands the structural links between code elements, it is arguably the best "forensic" tool for legacy Java systems, as it can trace dependency chains that would take hours to map manually.

### Common Doxygen Command-Line Usages

Unlike many modern CLI tools that take dozens of flags (`--output`, `--recursive`, etc.), Doxygen is designed to be **config-file-driven**. This is intentional; it keeps your build process repeatable and auditable.

| Command | Purpose |
| :--- | :--- |
| `doxygen -g` | **Generate:** Creates a default `Doxyfile` in your current folder. |
| `doxygen <filename>` | **Build:** Runs the analysis using a specific configuration file. |
| `doxygen -u <filename>` | **Update:** Upgrades an old `Doxyfile` to the latest version, preserving your settings. |
| `doxygen -v` | **Version:** Displays the version (good for checking if your PATH is set correctly). |
| `doxygen -s` | **Strip:** Used with `-g` or `-u` to create a "clean" config file without all the comments (great for sharing/audits). |
| `doxygen -` | **Pipe:** Reads the configuration from `stdin` instead of a file (the "patching" trick we used for dummy runs). |

---

### The "Forensic" Workflow Summary

For a project of your scale, you are moving away from the "default" usage and into a more **Surgical Workflow**:

1. **Configuration:** You don't use `doxygen -g` anymore. You maintain a handful of specialized `.doxconfig` files for different purposes (e.g., `audit.doxconfig`, `fast.doxconfig`).
2. **Environment:** You treat Doxygen as a standalone binary (pointing it via PATH or full path in `tasks.json`) rather than an extension.
3. **The "Heavy" Build:** You run this as a controlled process, typically using an overnight batch job or a `Ctrl+Shift+B` task that you specifically trigger, ensuring you aren't fighting your IDE for CPU cycles.
4. **Verification:** You review the `WARN_LOGFILE` after every run to ensure the "forensic map" you are generating is accurate and complete, rather than just trusting the HTML output.

By keeping the configuration deterministic and the output directory outside your source tree, you've essentially built a professional-grade documentation pipeline that is entirely air-gapped and immune to any "AI" interference.

## Usage

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

You are right to be wary. While Doxygen has evolved significantly (switching to STL containers and dropping some older internal caching complexities in modern versions), it is **not** a streaming, disk-only engine. It fundamentally wants to hold as much of your project’s symbol tree in memory as possible to perform the complex cross-referencing required for your call graphs.

### How it handles your large codebase

* **The "Symbol Tree":** When you run Doxygen, it builds an internal representation (an Abstract Syntax Tree of sorts) of every class, method, and variable in the `INPUT` path. This lives in RAM.
* **The Caching Mechanism:** Doxygen does have an internal "Lookup Cache" (`LOOKUP_CACHE_SIZE`). When it fills up, it pushes symbols to disk and retrieves them as needed. This is how it avoids crashing on massive projects, but it trades **memory usage** for **disk I/O**.
* **The Bottleneck:** If your project is truly massive and your `LOOKUP_CACHE_SIZE` is too low, Doxygen spends its entire time swapping data between RAM and the disk (this is the "thrashing" mentioned in the logs). If it's too high, you might hit the OOM (Out of Memory) limit of your system.

### Why you (hopefully) won't crash

1. **64-bit Architectures:** Since you're running on a modern machine, you have a much higher memory ceiling than developers did in the early 2000s. Doxygen can comfortably address 16GB, 32GB, or more of RAM, which is usually enough for even very large enterprise Java projects.
2. **Modern STL:** Doxygen’s move to standard STL containers (like `std::vector`, `std::map`) has made its memory footprint much more predictable and stable compared to the old custom "qtools" libraries.
3. **The "Safety Valve":** If you do run out of RAM, the OS will start "swapping" memory to your page file. The process won't necessarily *crash*, but it will slow down to a crawl. If you see your disk usage spike and your CPU usage drop during the process, that's your sign that it's thrashing.

### The "Overnight" Recommendation

To ensure your "let it loose" run is successful:

1. **SSD is Mandatory:** Ensure your project source and your output directory are both on an NVMe or fast SSD. Mechanical hard drives will absolutely choke during the "patching" and "caching" phases of a large build.
2. **Clear the Output First:** If you are using `DOT_CLEANUP = NO` for incremental builds, **delete the `/docs/doxygen` folder manually before you start the overnight run.** This ensures you aren't fighting with cached files from your smaller "dummy" runs.
3. **Check your Task Manager:** Before you walk away, start the process and watch the "Memory" and "Disk" columns in Task Manager for 2–3 minutes. If memory usage stays flat and Disk I/O is steady, you're in the clear.

**You are ready.** You’ve done the surgical planning that most developers skip, and you’ve set up the "safety valves." Let it run, check that `doxygen_warnings.log` in the morning, and enjoy the forensic map of your framework.

That stall at **25,081/42,576** confirms exactly what we feared: **Graphviz (dot) "Explosion."**

When Doxygen says it’s "Running dot," it is spawning a separate process to draw a single diagram. In a 40-year-old codebase, you likely hit a "God Class" or a deeply nested dependency where the resulting graph was so massive that Graphviz either ran out of memory or entered an infinite calculation loop trying to position the nodes.

Here is the "Forensic Rescue Plan" to get this build across the finish line.

### 1. Fix the "Dot" Hang (The Priority)

The fact that it stalled during graph generation means the parser finished, but the drawer (Graphviz) choked. You need to put "blinkers" on the graphing engine:

```ini
# --- Add these to your .doxconfig ---
DOT_GRAPH_MAX_NODES    = 50    # Absolute must. Prevents 1,000-node "spaghetti" graphs.
MAX_DOT_GRAPH_DEPTH    = 3     # Limit the "blast radius" of calls.
DOT_MULTI_TARGETS      = YES   # Allows dot to work more efficiently.
```

### 2. Address the "Not an Input File" Warnings

Doxygen is a bit pedantic about the `\file` command in Java.

* **The Cause:** Java developers often copy-paste headers. If a file named `UserDao.java` has a comment block saying `\file Service.java`, Doxygen panics because it can't find `Service.java` inside `UserDao.java`.

* **The Fix:** Unless you are manually documenting C-style header files, **don't use `\file` in Java.** You can tell Doxygen to ignore these misplaced tags:

    ```ini
    # Turn off the noise
    WARN_IF_DOC_ERROR      = NO
    ```

### 3. The "String" & Native Class Warnings

Doxygen is complaining because it doesn't know what a `String`, `List`, or `Map` is—it only knows about the code you gave it in the `INPUT` folder.

* **The Fix:** You need to tell Doxygen where the Java Standard Library documentation lives.

    ```ini
    # Add this to link to the official Java docs
    TAGFILES += "C:/DevTools/Doxygen/Tags/jdk17.tag=https://docs.oracle.com/en/java/javase/17/docs/api/"
    ```

    *(Note: You can download these `.tag` files so it stays 100% local/air-gapped.)*

### 4. The "Resume" Strategy

Since the parser actually finished (it was in the "Running dot" phase), you don't necessarily have to re-parse everything.

1. **Kill any lingering `dot.exe` processes** in Task Manager.
2. Update your config with the `DOT_GRAPH_MAX_NODES = 50` limit.
3. Set `DOT_CLEANUP = NO`.
4. Run it again. Doxygen will see the existing XML/Map files and only try to re-render the graphs that failed.

---

### My Recommendation: The "Graph-Lite" Run

If you just want to see the results today without another overnight wait, try this:

1. Set `HAVE_DOT = NO` just to see if the HTML generates successfully.
2. If the HTML looks good and the "Services" are all there, then turn `HAVE_DOT = YES` back on, but with the **Max Nodes = 50** limit.

**Would you like me to help you find a local copy of the Java JDK tag file so you can silence those "unable to resolve link" warnings without hitting the internet?**

It’s a common misconception that the JDK comes with a "Doxygen-ready" tag file. Unfortunately, **it does not**. The Javadoc tool and Doxygen are separate ecosystems; while Doxygen can parse Javadoc-style comments, it doesn't automatically know how to link to the standard library without a specifically formatted `.tag` file.

Since you are maintaining a strict "no-internet" policy, you have two options to silence those warnings and get your links working:

### Option 1: The "Manual Link" (Easiest)

If you don't actually need the documentation for `String` or `List` to be clickable, but you just want the **warnings to stop**, you can explicitly tell Doxygen to ignore them:

```ini
# Add these to your .doxconfig
# This tells Doxygen not to try to resolve links to things it doesn't know
WARN_IF_UNDOCUMENTED   = NO
WARN_NO_PARAMDOC       = NO
```

This is the "sane" approach for a 40-year-old codebase—you likely don't need documentation for `java.lang.String` anyway.

### Option 2: The "Air-Gapped Tag File" (The Hard Way)

If you truly want those links to work, you have to build the tag file yourself.

1. **Download the Javadoc HTML** for your JDK version (e.g., JDK 17, 21, etc.) as a ZIP from Oracle or OpenJDK.
2. **Extract it** to a local folder (e.g., `C:\DevTools\JDKDocs\17`).
3. **Run Doxygen on the Javadoc:** You have to trick Doxygen into "reading" the HTML docs to build the index.
    * This is actually quite difficult to get right, which is why most veteran devs avoid it.

### My Recommendation for your Forensics

Don't waste time generating a JDK tag file. In a forensic investigation of a legacy application, the **Standard Library** (`java.lang`, `java.util`) is rarely the source of your problems. The "spaghetti" is almost always in your own service layers, DAO wrappers, and legacy framework code.

**Keep your focus on your code:**

1. **Silence the noise:** Use the `WARN_IF_... = NO` settings above.
2. **Focus on the "Dot" stall:** That is your real "forensic" blocker. Once you add `DOT_GRAPH_MAX_NODES = 50` and `MAX_DOT_GRAPH_DEPTH = 3`, Doxygen will stop choking on the complexity of your system.

**Are you ready to kill those warnings and try the "Graph-Lite" build one more time?** I can help you finalize the exact `doxconfig` block to ensure you get a clean build on the next pass.
