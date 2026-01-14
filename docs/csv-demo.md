# CSV Demo - HyperSQL Integration

## Overview

The CSV Demo vapp (`SpecCSVDemo`) demonstrates three different approaches to working with CSV files in HyperSQL, showcasing the flexibility and power of embedded database integration in VirtualDesktop.

## Three Approaches to CSV Data

### 1. TEXT TABLE - Direct CSV Querying

**What it is**: HyperSQL's TEXT TABLE feature allows you to query CSV files directly as if they were database tables, without importing the data.

**Advantages**:
- No import step required
- Changes to the CSV file are immediately reflected in queries
- Zero storage overhead in the database
- Perfect for read-only data or files maintained externally

**Example**:
```sql
CREATE TEXT TABLE products_text (
  id INT,
  name VARCHAR(100),
  category VARCHAR(50),
  price DECIMAL(10,2),
  stock INT
);

SET TABLE products_text SOURCE 'data.csv;ignore_first=true;encoding=UTF-8;fs=,';

SELECT category, COUNT(*), AVG(price)
FROM products_text
GROUP BY category;
```

**Use Cases**:
- Reporting on log files
- Analyzing data exports from other systems
- Working with configuration files
- Quick data exploration without commitment

### 2. OpenCSV - Programmatic CSV Access

**What it is**: OpenCSV library (already in dependencies) provides a simple, programmatic way to read and write CSV files in Java.

**Advantages**:
- Full control over CSV parsing
- Easy to integrate into custom code
- Can handle complex CSV formats
- Great for ETL processes

**Example**:
```java
CSVReader reader = new CSVReader(new FileReader("data.csv"));
String[] header = reader.readNext();
String[] row;
while ((row = reader.readNext()) != null) {
    // Process each row
    System.out.println(Arrays.toString(row));
}
reader.close();
```

**Use Cases**:
- Custom data validation before import
- Transforming data during import
- Generating CSV reports from database queries
- Building data migration tools

### 3. CSV Import - Convert to Database Tables

**What it is**: Import CSV data into regular HyperSQL tables for full database functionality.

**Advantages**:
- Full SQL capabilities (indexes, foreign keys, triggers)
- Better performance for complex queries
- Data persistence in database format
- Can modify data after import

**Example**:
```java
// Create table
CREATE TABLE products_imported (
  id INT PRIMARY KEY,
  name VARCHAR(100),
  category VARCHAR(50),
  price DECIMAL(10,2),
  stock INT
);

// Read CSV and insert
CSVReader reader = new CSVReader(new FileReader("data.csv"));
reader.readNext(); // Skip header
String[] row;
while ((row = reader.readNext()) != null) {
    // Insert each row
    INSERT INTO products_imported VALUES (?, ?, ?, ?, ?);
}
```

**Use Cases**:
- Building application databases from CSV seed data
- Historical data archival
- Data requiring frequent updates
- Complex querying with indexes

## Using the Demo

### Quick Start

1. **Launch the vapp**: Select "CSV Demo" from the VApps menu
2. **Create sample data**: Click "1. Create Sample CSV"
3. **Try each approach**: Work through buttons 2-5 in order

### Step-by-Step Walkthrough

**Step 1: Create Sample CSV**
- Generates `data/sample-products.csv` with 8 sample products
- Contains: id, name, category, price, stock columns
- Or use "Choose CSV File..." to select your own CSV

**Step 2: TEXT TABLE Demo**
- Creates a TEXT TABLE linked to the CSV file
- Runs a GROUP BY query showing category statistics
- Demonstrates querying CSV without import

**Step 3: OpenCSV Read Demo**
- Uses OpenCSV library to read the CSV programmatically
- Shows all records with proper parsing
- Demonstrates programmatic access pattern

**Step 4: Import CSV to Table**
- Creates a regular HyperSQL table
- Imports all CSV data into the table
- Shows the import process step-by-step

**Step 5: Query Imported Table**
- Queries the imported table
- Displays all records in formatted output
- Demonstrates regular SQL on imported data

### Working with Your Own CSV Files

1. Click "Choose CSV File..." to select any CSV file
2. Follow steps 2-5 to work with your data
3. Modify the column definitions in the code if needed

## Technical Details

### HyperSQL TEXT TABLE Configuration

The SOURCE clause for TEXT TABLE supports these options:

```
SET TABLE tablename SOURCE 'filename;options'
```

**Common options**:
- `ignore_first=true` - Skip header row
- `encoding=UTF-8` - Character encoding
- `fs=,` - Field separator (comma)
- `vs="` - Value separator (quote)
- `qc=\"` - Quote character
- `compressed=true` - For gzipped CSV files

### OpenCSV Features

OpenCSV (version 2.1) supports:
- Custom separators and quote characters
- Automatic type conversion
- Header mapping
- Streaming large files
- Writing CSV with proper escaping

### Performance Considerations

**TEXT TABLE**:
- Reads from disk on each query
- Good for small-medium files (<100MB)
- No index support
- Best for read-only scenarios

**Imported Tables**:
- Data in memory or disk (depending on HyperSQL settings)
- Full index support for fast queries
- Requires storage space
- Best for frequently queried data

## Future Enhancements

This demo provides a foundation for:

1. **H2 Database Integration** - H2 has even better CSV support via CSVREAD/CSVWRITE functions
2. **Batch Import Tools** - Import multiple CSV files at once
3. **CSV Export** - Export query results to CSV (already available via CSVWriter)
4. **Data Validation** - Validate CSV data before import
5. **Schema Detection** - Auto-detect column types from CSV data

## Related Documentation

- [HyperSQL User Guide - TEXT Tables](http://hsqldb.org/doc/guide/texttables-chapt.html)
- [OpenCSV Documentation](http://opencsv.sourceforge.net/)
- [Database Capabilities Summary](../CLAUDE.md#database-capabilities)

## Code Location

- Vapp: `src/main/java/org/jwellman/virtualdesktop/vapps/SpecCSVDemo.java`
- Config: Entry in `config/vapps-config.json`
- Dependencies: OpenCSV 2.1 (already in `pom.xml`)

## Notes

- The demo uses an in-memory database (`jdbc:hsqldb:mem:csvdemo`)
- Data is lost when the vapp is closed
- Sample CSV file is created in `data/sample-products.csv`
- TEXT TABLEs require absolute file paths
- OpenCSV 2.1 is Java 8 compatible
