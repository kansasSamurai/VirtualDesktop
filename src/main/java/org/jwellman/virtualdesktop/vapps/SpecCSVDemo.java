package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Demonstrates HyperSQL CSV capabilities using three approaches:
 * 1. TEXT TABLE - Direct CSV querying without import
 * 2. OpenCSV - Programmatic CSV reading/writing
 * 3. CSV to Table Import - Convert CSV files to regular HyperSQL tables
 *
 * @author Rick Wellman
 */
public class SpecCSVDemo extends VirtualAppSpec {

    private static final String DRIVER = "org.hsqldb.jdbc.JDBCDriver";
    private static final String DB_URL = "jdbc:hsqldb:file:data/csvdemo;shutdown=true";
    private static final String DB_USER = "SA";
    private static final String DB_PASSWORD = "";

    private JTextArea outputArea;
    private Connection conn;
    private File lastCsvFile;

    public SpecCSVDemo() {
        this.setTitle("CSV Demo - HyperSQL");

        try {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            log("Database connection established (file-based: data/csvdemo)");
            log("Note: Database files will be created in data/ directory\n");
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.setContent(this.createDefaultContent(createUI()));
    }

    private JPanel createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Output area
        outputArea = new JTextArea(20, 60);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        // Sample data creation
        JButton createSampleBtn = new JButton("1. Create Sample CSV");
        createSampleBtn.addActionListener(e -> createSampleCSV());
        buttonPanel.add(createSampleBtn);

        // TEXT TABLE demo
        JButton textTableBtn = new JButton("2. TEXT TABLE Demo");
        textTableBtn.addActionListener(e -> demonstrateTextTable());
        buttonPanel.add(textTableBtn);

        // OpenCSV demo
        JButton openCsvBtn = new JButton("3. OpenCSV Read Demo");
        openCsvBtn.addActionListener(e -> demonstrateOpenCSV());
        buttonPanel.add(openCsvBtn);

        // Import CSV to table
        JButton importBtn = new JButton("4. Import CSV to Table");
        importBtn.addActionListener(e -> importCsvToTable());
        buttonPanel.add(importBtn);

        // Query imported table
        JButton queryBtn = new JButton("5. Query Imported Table");
        queryBtn.addActionListener(e -> queryImportedTable());
        buttonPanel.add(queryBtn);

        // Choose CSV file
        JButton chooseFileBtn = new JButton("Choose CSV File...");
        chooseFileBtn.addActionListener(e -> chooseCsvFile());
        buttonPanel.add(chooseFileBtn);

        // Clear output
        JButton clearBtn = new JButton("Clear Output");
        clearBtn.addActionListener(e -> outputArea.setText(""));
        buttonPanel.add(clearBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Initial instructions
        log("=== HyperSQL CSV Demonstration ===\n");
        log("This demo shows three ways to work with CSV files:\n");
        log("1. TEXT TABLE - Query CSV files directly (no import needed)");
        log("2. OpenCSV - Read/write CSV files programmatically");
        log("3. Import - Convert CSV to regular HyperSQL tables\n");
        log("Click '1. Create Sample CSV' to begin, or 'Choose CSV File' to use your own.\n");

        return mainPanel;
    }

    /**
     * Create a sample CSV file for demonstration
     */
    private void createSampleCSV() {
        try {
            File csvFile = new File("data/sample-products.csv");
            csvFile.getParentFile().mkdirs();

            CSVWriter writer = new CSVWriter(new FileWriter(csvFile));

            // Write header
            String[] header = {"id", "name", "category", "price", "stock"};
            writer.writeNext(header);

            // Write sample data
            writer.writeNext(new String[]{"1", "Laptop", "Electronics", "899.99", "15"});
            writer.writeNext(new String[]{"2", "Mouse", "Electronics", "24.99", "150"});
            writer.writeNext(new String[]{"3", "Keyboard", "Electronics", "79.99", "75"});
            writer.writeNext(new String[]{"4", "Monitor", "Electronics", "299.99", "30"});
            writer.writeNext(new String[]{"5", "Desk Chair", "Furniture", "189.99", "45"});
            writer.writeNext(new String[]{"6", "Standing Desk", "Furniture", "449.99", "20"});
            writer.writeNext(new String[]{"7", "Notebook", "Office Supplies", "4.99", "500"});
            writer.writeNext(new String[]{"8", "Pen Set", "Office Supplies", "12.99", "200"});

            writer.close();

            lastCsvFile = csvFile;
            log("✓ Sample CSV created: " + csvFile.getAbsolutePath());
            log("  Columns: id, name, category, price, stock");
            log("  Records: 8 sample products\n");

        } catch (Exception e) {
            log("ERROR creating sample CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Demonstrate HyperSQL TEXT TABLE feature for direct CSV querying
     */
    private void demonstrateTextTable() {
        if (lastCsvFile == null || !lastCsvFile.exists()) {
            log("ERROR: No CSV file selected. Create sample or choose a file first.\n");
            return;
        }

        try {
            log("=== TEXT TABLE Demonstration ===");
            log("Creating TEXT TABLE linked to: " + lastCsvFile.getName() + "\n");

            Statement stmt = conn.createStatement();

            // Drop if exists
            try {
                stmt.execute("DROP TABLE products_text IF EXISTS");
            } catch (Exception e) {
                // Ignore if doesn't exist
            }

            // Create TEXT TABLE
            String createSql = "CREATE TEXT TABLE products_text (" +
                    "id INT, " +
                    "name VARCHAR(100), " +
                    "category VARCHAR(50), " +
                    "price DECIMAL(10,2), " +
                    "stock INT)";
            stmt.execute(createSql);
            log("✓ TEXT TABLE created");

            // Link to CSV file
            String absolutePath = lastCsvFile.getAbsolutePath().replace("\\", "/");
            String sourceSql = "SET TABLE products_text SOURCE '" +
                    absolutePath + ";ignore_first=true;encoding=UTF-8;fs=,;vs=\";qc=\"'";
            stmt.execute(sourceSql);
            log("✓ Linked to CSV file\n");

            // Query the TEXT TABLE
            log("Querying TEXT TABLE (reads directly from CSV):");
            ResultSet rs = stmt.executeQuery(
                    "SELECT category, COUNT(*) as count, AVG(price) as avg_price " +
                    "FROM products_text " +
                    "GROUP BY category " +
                    "ORDER BY category");

            log(String.format("%-20s %8s %12s", "Category", "Count", "Avg Price"));
            log(String.format("%-20s %8s %12s", "--------", "-----", "---------"));

            while (rs.next()) {
                log(String.format("%-20s %8d %12.2f",
                        rs.getString("category"),
                        rs.getInt("count"),
                        rs.getDouble("avg_price")));
            }

            log("\n✓ TEXT TABLE allows querying CSV files without importing!");
            log("  Changes to the CSV file are reflected in queries.\n");

            rs.close();
            stmt.close();

        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Demonstrate OpenCSV library for programmatic CSV reading
     */
    private void demonstrateOpenCSV() {
        if (lastCsvFile == null || !lastCsvFile.exists()) {
            log("ERROR: No CSV file selected. Create sample or choose a file first.\n");
            return;
        }

        try {
            log("=== OpenCSV Demonstration ===");
            log("Reading CSV file: " + lastCsvFile.getName() + "\n");

            CSVReader reader = new CSVReader(new FileReader(lastCsvFile));

            String[] header = reader.readNext();
            log("Header: " + String.join(", ", header) + "\n");

            log("Records:");
            String[] row;
            int count = 0;
            while ((row = reader.readNext()) != null) {
                count++;
                log("  " + count + ". " + String.join(" | ", row));
            }

            reader.close();

            log("\n✓ OpenCSV allows easy programmatic access to CSV data");
            log("  Total records read: " + count + "\n");

        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Import CSV file into a regular HyperSQL table
     */
    private void importCsvToTable() {
        if (lastCsvFile == null || !lastCsvFile.exists()) {
            log("ERROR: No CSV file selected. Create sample or choose a file first.\n");
            return;
        }

        try {
            log("=== CSV Import to Table ===");
            log("Importing: " + lastCsvFile.getName() + "\n");

            Statement stmt = conn.createStatement();

            // Drop if exists
            try {
                stmt.execute("DROP TABLE products_imported IF EXISTS");
            } catch (Exception e) {
                // Ignore
            }

            // Create regular table
            String createSql = "CREATE TABLE products_imported (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "category VARCHAR(50), " +
                    "price DECIMAL(10,2), " +
                    "stock INT)";
            stmt.execute(createSql);
            log("✓ Table 'products_imported' created");

            // Read CSV and insert data
            CSVReader reader = new CSVReader(new FileReader(lastCsvFile));
            reader.readNext(); // Skip header

            String[] row;
            int count = 0;
            while ((row = reader.readNext()) != null) {
                String insertSql = String.format(
                        "INSERT INTO products_imported VALUES (%s, '%s', '%s', %s, %s)",
                        row[0], row[1], row[2], row[3], row[4]);
                stmt.execute(insertSql);
                count++;
            }

            reader.close();

            log("✓ Imported " + count + " records");
            log("\n✓ CSV data now in regular HyperSQL table");
            log("  Use '5. Query Imported Table' to see the data\n");

            stmt.close();

        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Query the imported table
     */
    private void queryImportedTable() {
        try {
            log("=== Query Imported Table ===\n");

            Statement stmt = conn.createStatement();

            // Show all products
            log("All products in imported table:");
            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM products_imported ORDER BY category, name");

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // Print header
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                header.append(String.format("%-15s ", meta.getColumnName(i)));
            }
            log(header.toString());
            log(String.format("%-15s %-15s %-15s %-15s %-15s",
                    "---", "---", "---", "---", "---"));

            // Print rows
            while (rs.next()) {
                log(String.format("%-15s %-15s %-15s %-15.2f %-15d",
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("stock")));
            }

            log("\n✓ Data is now in a regular table");
            log("  You can create indexes, foreign keys, triggers, etc.\n");

            rs.close();
            stmt.close();

        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Allow user to choose a CSV file
     */
    private void chooseCsvFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
            }
            public String getDescription() {
                return "CSV Files (*.csv)";
            }
        });

        if (lastCsvFile != null) {
            chooser.setCurrentDirectory(lastCsvFile.getParentFile());
        }

        int result = chooser.showOpenDialog(this.getContent());
        if (result == JFileChooser.APPROVE_OPTION) {
            lastCsvFile = chooser.getSelectedFile();
            log("Selected CSV file: " + lastCsvFile.getAbsolutePath() + "\n");
        }
    }

    /**
     * Log output to the text area
     */
    private void log(String message) {
        outputArea.append(message + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    /**
     * Cleanup on close
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.finalize();
    }
}
