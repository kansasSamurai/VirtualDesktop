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
