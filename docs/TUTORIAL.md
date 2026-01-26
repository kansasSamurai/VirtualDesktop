# VirtualDesktop Tutorial

A comprehensive guide to using VirtualDesktop - your Java-based virtual desktop environment.

---

## Table of Contents

- [Part 1: Getting Started](#part-1-getting-started)
  - [1. Welcome to VirtualDesktop](#1-welcome-to-virtualdesktop)
  - [2. Desktop Tour](#2-desktop-tour)
- [Part 2: BeanShell Scripting](#part-2-beanshell-scripting)
  - [3. BeanShell Console Basics](#3-beanshell-console-basics)
  - [4. The env() Environment](#4-the-env-environment)
  - [5. Built-in Services](#5-built-in-services)
- [Part 3: Built-in Applications](#part-3-built-in-applications)
  - [6. Data Tools](#6-data-tools)
  - [7. Visualization](#7-visualization)
  - [8. Utilities](#8-utilities)
- [Part 4: Configuration & Customization] #part-4-configuration--customization 
  - [9. Theming](#9-theming)
  - [10. Menu Customization](#10-menu-customization)
  - [11. Database Configuration](#11-database-configuration)
  - [12. External Applications](#12-external-applications)
- [Part 5: Advanced Topics](#part-5-advanced-topics)
  - [13. Writing Custom Scripts](#13-writing-custom-scripts)
  - [14. Groovy Integration](#14-groovy-integration)
- [Appendices](#appendices)
  - [A: Quick Reference Card](#appendix-a-quick-reference-card)
  - [B: Configuration File Schemas](#appendix-b-configuration-file-schemas)
  - [C: Troubleshooting](#appendix-c-troubleshooting)

---

# Part 1: Getting Started

## 1. Welcome to VirtualDesktop

VirtualDesktop is a Java-based desktop environment that runs entirely within a single JVM. Unlike remote desktop software, VirtualDesktop provides a consistent, cross-platform desktop metaphor with integrated development tools and utilities.

### What You Can Do

- Run multiple "vapps" (virtual applications) in internal windows
- Use BeanShell for interactive scripting and automation
- Work with databases, CSV files, and web resources
- Visualize data with charting tools
- Customize themes and layouts

### Launching the Application

```bash
# From the project root
mvn clean compile exec:java -Dexec.mainClass="org.jwellman.virtualdesktop.App"

# Or run the packaged JAR
java -jar virtualdesktop.jar
```

![VirtualDesktop main window](screenshots/placeholder-main-window.png)

*Coming soon: Screenshot of the main VirtualDesktop window*

---

## 2. Desktop Tour

### The Menu System

- **File Menu** - Application management, exit
- **Vapps Menu** - Launch virtual applications
- **Themes Menu** - Change look and feel
- **Window Menu** - Manage open windows

### Desktop Shortcuts

Desktop shortcuts (VShortcuts) can be:
- Double-clicked to launch applications
- Dragged and repositioned
- Saved/loaded with desktop layout

### Window Management

- **Minimize/Maximize** - Standard window controls
- **Tile** - Arrange windows automatically
- **Cascade** - Stack windows diagonally

![Desktop overview](screenshots/placeholder-desktop-overview.png)

*Coming soon: Annotated screenshot showing menu bar, desktop icons, and vapps*

---

# Part 2: BeanShell Scripting

## 3. BeanShell Console Basics

The BeanShell console is a powerful interactive scripting environment embedded in VirtualDesktop. It allows you to write and execute Java-like code on the fly.

### Opening the BeanShell Console

1. From the menu bar, select **Vapps > BeanShell**
2. A new internal window opens with the BeanShell interpreter

![BeanShell console window](screenshots/placeholder-console-window.png)

### Your First Commands

The BeanShell syntax is essentially Java, but with some convenience features:

```java
// Print a message
print("Hello, VirtualDesktop!");

// Create variables (no type declaration required)
message = "Hello";
count = 42;

// Use Java classes directly
date = new java.util.Date();
print(date);

// Create a list
items = new java.util.ArrayList();
items.add("one");
items.add("two");
print(items);
```

### Basic Arithmetic

```java
// Simple calculations
result = 10 + 5;
print(result);  // 15

// Java Math class
sqrt = Math.sqrt(16);
print(sqrt);  // 4.0
```

### Defining Functions

```java
// Define a simple function
greet(name) {
    print("Hello, " + name + "!");
}

// Call it
greet("World");  // Hello, World!
```

---

## 4. The env() Environment

The `env()` function is the cornerstone of the VirtualDesktop scripting environment. It provides a rich set of utilities and services for interactive work.

### Activating the Environment

To load the environment, simply call:

```java
env = env();
```

When you do this, you'll see a welcome banner and the help output:

```
╒════════════════════════════════
│ Defining env() ...
│════════════════════════════════
│
│ Methods:
│   d() ... display()
│   v() ... variables()
│   m() ... methods()
│   methodnames()
│   sortandprint*(String[])
│ Services:
│   web      - Remote resource management
│   files    - File system and path resolution
│   db       - Database connection management
│
│════════════════════════════════
│ Reminder: env = env()
 ════════════════════════════════
```

### Built-in Help

After loading the environment, call `help()` anytime to see available commands:

```java
help();
```

### Introspection Commands

The env provides several commands to explore your current session:

#### display() or d()

Shows both variables and methods in the current namespace:

```java
d();
// Or the full name:
display();
```

Output:
```
╒════════════════════════════════
│   Variables
 ════════════════════════════════
db
env
files
web
... (other variables you've created)

╒════════════════════════════════
│   Methods
 ════════════════════════════════
[method definitions...]

Note: Use methodnames() to get *just* method names
```

#### variables() or v()

Lists only the variables in the global namespace:

```java
v();
```

#### methods() or m()

Shows all methods with their full signatures:

```java
m();
```

#### methodnames()

Shows just the method names (without signatures), sorted alphabetically:

```java
methodnames();
```

### Understanding the Services

After calling `env()`, three service objects are automatically available in the global namespace:

| Service | Purpose |
|---------|---------|
| `files` | File system operations, CSV reading |
| `web`   | Downloading files, HTTP requests |
| `db`    | Database connections and queries |

These services work together. For example, `web.download()` uses `files.resolve()` to determine where to save files.

---

## 5. Built-in Services

### 5.1 The `files` Service

The `files` service provides a stable, scoped namespace for filesystem operations. It maintains a "base directory" that relative paths are resolved against.

#### Setting the Base Directory

```java
// Set where relative paths resolve from
files.setBase("C:/data/projects");
// Output: ✓ Files: Base directory updated to: C:/data/projects

// Check current base (it's stored in files.baseDir)
print(files.baseDir);
```

By default, the base directory is the JVM's working directory (`user.dir`).

#### Checking File Existence

```java
// Returns true/false without throwing exceptions
if (files.exists("mydata.csv")) {
    print("File found!");
} else {
    print("File not found.");
}
```

#### Resolving File Paths

The `resolve()` method converts a filename to an absolute path. It throws an exception if the file doesn't exist:

```java
// Get absolute path (throws if not found)
path = files.resolve("mydata.csv");
print(path);  // C:/data/projects/mydata.csv

// Absolute paths pass through unchanged
path = files.resolve("C:/other/location/file.txt");
```

#### Reading CSV Files

The `readCSV()` method parses a CSV file and returns a `List<String[]>`:

```java
// Read a CSV file
rows = files.readCSV("sales_data.csv");
// Output: ✓ Files: Loaded 1500 rows.

// Access the header row
header = rows.get(0);
print(Arrays.toString(header));  // [Date, Product, Quantity, Price]

// Iterate through data rows
for (i = 1; i < rows.size(); i++) {
    row = rows.get(i);
    print(row[0] + ": " + row[1] + " x " + row[2]);
}
```

#### Creating Files from Providers

The `create()` method works with "provider" objects that have a `getStream()` method:

```java
// Create a file from a web download provider
provider = web.download("https://example.com/data.csv");
localPath = files.create(provider, "local_data.csv");
// Output: ✓ Files: Created local_data.csv
```

#### Complete Example: CSV Processing

```java
// Initialize environment
env = env();

// Set up working directory
files.setBase("C:/data/analysis");

// Check if our data exists
if (!files.exists("quarterly_report.csv")) {
    print("Error: Report file not found!");
    return;
}

// Load and process the CSV
data = files.readCSV("quarterly_report.csv");

// Skip header, sum the third column
total = 0.0;
for (i = 1; i < data.size(); i++) {
    row = data.get(i);
    value = Double.parseDouble(row[2]);
    total += value;
}

print("Total: " + total);
```

### 5.2 The `web` Service

The `web` service handles remote data retrieval and integrates seamlessly with the `files` service.

#### Creating a Download Provider

The `download(url)` method (single argument) creates a "provider" object that can be passed to `files.create()`:

```java
// Create a provider (doesn't download yet)
provider = web.download("https://example.com/dataset.csv");

// The provider has a getStream() method for deferred downloading
// Use it with files.create():
localPath = files.create(provider, "dataset.csv");
```

#### Direct Download to File

The `download(url, filename)` method (two arguments) downloads immediately:

```java
// Download directly to a file
path = web.download(
    "https://raw.githubusercontent.com/datasets/gdp/master/data/gdp.csv",
    "gdp_data.csv"
);
// Output:
// → Web: Starting download from https://...
// ✓ Web: Download complete -> C:/data/gdp_data.csv

// The returned path can be used directly
data = files.readCSV(path);
```

#### Fetching Text Content

The `fetch(url)` method performs a GET request and returns the content as a String:

```java
// Fetch JSON data
jsonText = web.fetch("https://api.example.com/data.json");
print(jsonText);

// Fetch and display a text file
content = web.fetch("https://example.com/readme.txt");
print(content);
```

#### Controlling Verbosity

Both services have a `verbose` flag:

```java
// Turn off status messages
web.verbose = false;
files.verbose = false;

// Now operations are silent
web.download("https://example.com/file.csv", "file.csv");
```

#### Complete Example: Download and Process

```java
// Initialize environment
env = env();

// Set working directory
files.setBase("C:/data/downloads");

// Download GDP data from the web
url = "https://raw.githubusercontent.com/datasets/gdp/master/data/gdp.csv";

if (!files.exists("gdp_data.csv")) {
    print("Downloading GDP data...");
    web.download(url, "gdp_data.csv");
}

// Load the data
gdp = files.readCSV("gdp_data.csv");

// Display header
header = gdp.get(0);
print("Columns: " + Arrays.toString(header));

// Show first 5 data rows
print("\nFirst 5 records:");
for (i = 1; i <= 5 && i < gdp.size(); i++) {
    row = gdp.get(i);
    print("  " + row[0] + " | " + row[1] + " | $" + row[2]);
}

print("\nTotal rows: " + (gdp.size() - 1));
```

### 5.3 The `db` Service

The `db` service provides access to database connections configured in `dbconfig.json`.

#### Listing Available Connections

```java
db.list();
```

Output:
```
╒════════════════════════════════
│   Database Connections
 ════════════════════════════════
  LocalHSQL -> jdbc:hsqldb:file:./data/testdb
  DevMySQL -> jdbc:mysql://localhost:3306/devdb
  ProdPostgres -> jdbc:postgresql://prod-server/maindb
```

#### Getting Connection Configuration

```java
// Get config details for a named connection
config = db.getConfig("LocalHSQL");
print("Driver: " + config.getDriver());
print("URL: " + config.getUrl());
print("Username: " + config.getUsername());
```

#### Opening a Connection

```java
// Connect by name (returns java.sql.Connection)
conn = db.connect("LocalHSQL");
// Output: ✓ DB: Connected to LocalHSQL

// Now use standard JDBC
stmt = conn.createStatement();
rs = stmt.executeQuery("SELECT * FROM customers LIMIT 10");

while (rs.next()) {
    print(rs.getString("name") + " - " + rs.getString("email"));
}

rs.close();
stmt.close();
conn.close();
```

#### The Fluent API (use/query)

For convenience, the db service supports a fluent style:

```java
// Switch active connection
db.use("LocalHSQL");
// Output: ✓ DB: Connected to LocalHSQL

// Execute queries on the active connection
db.query("SELECT COUNT(*) FROM orders");
// Output: Executing: SELECT COUNT(*) FROM orders
```

*Note: The query() method is currently a placeholder that prints the SQL. Full result handling is planned.*

#### Reloading Configuration

If you modify `dbconfig.json` while the application is running:

```java
db.reload();
// Output: ✓ DB: Config reloaded

// Now db.list() shows updated connections
db.list();
```

#### Complete Example: Database Query

```java
// Initialize environment
env = env();

// List available databases
db.list();

// Connect to local HSQLDB
conn = db.connect("LocalHSQL");
if (conn == null) {
    print("Could not connect to database.");
    return;
}

// Create a statement and run a query
stmt = conn.createStatement();

// Example: Get all tables in the database
rs = stmt.executeQuery(
    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
    "WHERE TABLE_SCHEMA = 'PUBLIC'"
);

print("\nTables in database:");
while (rs.next()) {
    print("  - " + rs.getString(1));
}

// Clean up
rs.close();
stmt.close();
conn.close();
print("\nConnection closed.");
```

### Service Integration Example

Here's a complete workflow using all three services together:

```java
// Initialize the environment
env = env();

// Configure working directory
files.setBase("C:/data/analysis");

// Download data if we don't have it
if (!files.exists("population.csv")) {
    print("Downloading population data...");
    web.download(
        "https://example.com/datasets/population.csv",
        "population.csv"
    );
}

// Load the CSV data
data = files.readCSV("population.csv");
print("Loaded " + data.size() + " rows from CSV");

// Connect to database to store results
conn = db.connect("LocalHSQL");
stmt = conn.createStatement();

// Create table if needed
stmt.execute(
    "CREATE TABLE IF NOT EXISTS population_import (" +
    "  country VARCHAR(100), " +
    "  year INT, " +
    "  population BIGINT" +
    ")"
);

// Insert data (skip header row)
pstmt = conn.prepareStatement(
    "INSERT INTO population_import VALUES (?, ?, ?)"
);

for (i = 1; i < data.size(); i++) {
    row = data.get(i);
    pstmt.setString(1, row[0]);
    pstmt.setInt(2, Integer.parseInt(row[1]));
    pstmt.setLong(3, Long.parseLong(row[2]));
    pstmt.executeUpdate();
}

print("Imported " + (data.size() - 1) + " records to database.");

// Clean up
pstmt.close();
stmt.close();
conn.close();
```

---

# Part 3: Built-in Applications

## 6. Data Tools

### CSV Viewer

Load and view CSV files in a table format.

![CSV Viewer](screenshots/placeholder-csv-viewer.png)

*Coming soon: Details on using the CSV viewer vapp*

- Opening CSV files
- Column sorting
- Filtering rows
- Exporting subsets

### HyperSQL Database Manager

An embedded SQL database client for HSQLDB.

![HyperSQL Manager](screenshots/placeholder-hsqldb-manager.png)

*Coming soon: Tutorial on HyperSQL database operations*

- Connecting to databases
- Running SQL queries
- Viewing table structures
- Exporting results

---

## 7. Visualization

### JFreeChart

Create charts and graphs from data.

![JFreeChart demo](screenshots/placeholder-jfreechart.png)

*Coming soon: Chart creation tutorial*

- Line charts
- Bar charts
- Pie charts
- Customizing appearance

### XChart Demos

Alternative charting library demonstrations.

![XChart demo](screenshots/placeholder-xchart.png)

*Coming soon: XChart examples*

- Quick charts from data
- Real-time updating
- Export to image

---

## 8. Utilities

### JCX Console

A versatile command console.

![JCX Console](screenshots/placeholder-jcx-console.png)

*Coming soon: Console usage guide*

### Object Browser

Inspect Java objects at runtime.

*Coming soon: Object Browser tutorial*

- Navigating object trees
- Inspecting fields and methods
- Evaluating expressions

### Script Browser

Browse and run BeanShell scripts.

*Coming soon: Script Browser guide*

- Organizing scripts
- Running scripts from browser
- Script templates

---

# Part 4: Configuration & Customization

## 9. Theming

VirtualDesktop supports multiple Look and Feel options.

### Available Themes

| Theme | Description |
|-------|-------------|
| FlatLaf | Modern flat design (default) |
| JTattoo Aluminium | Mac-like appearance |
| WebLAF | Web-inspired look |
| Nimbus | Standard Java LAF |
| Metal | Classic Java appearance |
| Napkin | Sketch/hand-drawn style |
| System | Native OS appearance |

![Theme comparison](screenshots/placeholder-theme-comparison.png)

*Coming soon: Visual comparison of themes*

### Changing Themes

- Use the **Themes** menu to switch at runtime
- Some themes require application restart

---

## 10. Menu Customization

The vapps menu can be customized via `vapps-config.json`.

*Coming soon: Menu configuration guide*

- Adding menu items
- Creating submenus
- Keyboard shortcuts

See [vapps-config.md](vapps-config.md) for current documentation.

---

## 11. Database Configuration

Database connections are configured in `dbconfig.json`.

*Coming soon: Full database configuration guide*

### Basic Structure

```json
{
  "connections": [
    {
      "name": "LocalHSQL",
      "driver": "org.hsqldb.jdbc.JDBCDriver",
      "url": "jdbc:hsqldb:file:./data/mydb",
      "username": "SA",
      "password": ""
    }
  ]
}
```

- Connection properties
- Supported databases
- Connection pooling

---

## 12. External Applications

Launching external applications from VirtualDesktop.

*Coming soon: External app integration guide*

- Configuring external applications
- File associations
- Platform-specific considerations

---

# Part 5: Advanced Topics

## 13. Writing Custom Scripts

Create reusable BeanShell scripts for automation.

*Coming soon: Script development guide*

### Script Organization

- Where to save scripts
- Naming conventions
- Script templates

### Best Practices

- Error handling
- Modular design
- Testing scripts

---

## 14. Groovy Integration

Using the Groovy console for advanced scripting.

*Coming soon: Groovy tutorial*

### Opening Groovy Console

- Launching from menu
- JDK9+ requirements

### Groovy vs BeanShell

- When to use each
- Syntax differences
- Performance considerations

---

# Appendices

## Appendix A: Quick Reference Card

### Environment Initialization

```java
env = env();   // Load environment with services
```

### Introspection Commands

| Command | Alias | Description |
|---------|-------|-------------|
| `help()` | - | Show help text |
| `display()` | `d()` | Show variables and methods |
| `variables()` | `v()` | List all variables |
| `methods()` | `m()` | List all methods with signatures |
| `methodnames()` | - | List method names only |

### files Service

| Method | Description |
|--------|-------------|
| `files.setBase(path)` | Set working directory |
| `files.exists(name)` | Check if file exists (boolean) |
| `files.resolve(name)` | Get absolute path (throws if missing) |
| `files.readCSV(name)` | Read CSV as List<String[]> |
| `files.create(provider, name)` | Create file from provider |

### web Service

| Method | Description |
|--------|-------------|
| `web.download(url)` | Create download provider |
| `web.download(url, file)` | Download to local file |
| `web.fetch(url)` | GET request, return as String |

### db Service

| Method | Description |
|--------|-------------|
| `db.list()` | Print all configured connections |
| `db.getConfig(name)` | Get connection configuration |
| `db.connect(name)` | Open JDBC connection |
| `db.reload()` | Reload config from file |
| `db.use(name)` | Set active connection |
| `db.query(sql)` | Execute on active connection |

---

## Appendix B: Configuration File Schemas

### dbconfig.json

```json
{
  "connections": [
    {
      "name": "string (required) - unique identifier",
      "driver": "string (required) - JDBC driver class",
      "url": "string (required) - JDBC connection URL",
      "username": "string (optional) - database user",
      "password": "string (optional) - database password"
    }
  ]
}
```

### vapps-config.json

*See [vapps-config.md](vapps-config.md) for full schema*

---

## Appendix C: Troubleshooting

### BeanShell Issues

**Problem**: `env()` not found

**Solution**: Ensure the env.bsh script is on the classpath:
```
src/main/resources/org/jwellman/bsh/scripts/env.bsh
```

---

**Problem**: CSV parsing fails

**Solution**: Check that OpenCSV is on the classpath and the file path is correct:
```java
// Debug with exists() first
print(files.exists("myfile.csv"));
print(files.baseDir);
```

---

### Database Issues

**Problem**: Connection fails with "driver not found"

**Solution**: Ensure the JDBC driver JAR is on the classpath and the driver class name is correct in dbconfig.json.

---

**Problem**: `db.list()` shows no connections

**Solution**: Check that dbconfig.json exists in the working directory and is valid JSON:
```java
db.reload();
db.list();
```

---

### General Issues

**Problem**: Application won't start on JDK9+

**Solution**: Add the required VM argument:
```bash
--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
```

---

**Problem**: Vapp crashes the entire application

**Solution**: This is a known limitation. Each vapp runs in the same JVM, so an unhandled exception can affect everything. Save your work frequently and report issues.

---

*This tutorial is a work in progress. Sections marked "Coming soon" will be expanded in future updates.*
