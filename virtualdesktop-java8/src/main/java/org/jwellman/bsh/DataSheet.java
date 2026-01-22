package org.jwellman.bsh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jwellman.virtualdesktop.bsh.BeanShellService; 

/**
 * 2. The "Schema Sidecar" Vision
Your plan to use a companion file to automate the asDouble() calls is brilliant for two reasons:

Automation: The scripter doesn't have to write the "prep" lines every time.

Persistence: The knowledge that "Column 5 is a Currency" stays with the data, not just in a one-off script.

How the "Sidecar" Flow looks:

gdp_data.csv: Raw data.

gdp_data.csv.json (or .schema): A simple map like {"Value": "Double", "Year": "Int"}.

When your DataSheet constructor (or factory) runs, it looks for that file. If found, it automatically iterates through the mapping and applies the asDouble() or asInt() logic before the user even gets the object.

A Note on your "Sidecar Schema"
When you eventually build that sidecar file, you can just map a column to Number. The platform will see that and trigger the asNumber() logic automatically.

 */

/**
 * DataSheet provides a lightweight, in-memory representation of tabular data.
 * <p>
 * It serves as an abstraction layer between various data sources (SQL, CSV, Excel)
 * and scripting environments (BeanShell, JavaScript). By wrapping a {@code List<Map>},
 * it provides functional-style operations like {@code filter} and {@code sort}
 * that can be executed via compiled Java logic or dynamic scripts.
 * </p>
 * * <b>Key Features:</b>
 * <ul>
 * <li>Scriptable filtering using BeanShell closures or String expressions.</li>
 * <li>Native Java Stream performance for bulk data operations.</li>
 * <li>Schema-agnostic column mapping.</li>
 * </ul>
 * 
 * @author rwellman
 *
 */
public class DataSheet {

    /* A generic in-memory data model that is easily mapped to 
     * database resultsets, tabular file formats such as .csv, .xls, etc.  
     * As you can see this represents a collection of records, 
     * where each record is a Map of column names(String) to values(Object).
     */
    private List<Map<String, Object>> rows;

    /**
     * Create a DataSheet.
     * <p>
     * Future Proof: If you later decide to support a List<List<String>> or 
     * even a ResultSet, you just add one more else if block to this constructor.
     * 
     * @param input
     */
    @SuppressWarnings("unchecked")
    public DataSheet(List<?> input) {
        if (input == null || input.isEmpty()) {
            this.rows = new ArrayList<>();
            return;
        }

        Object firstElement = input.get(0);

        // Case 1: It's already the format we want (List of Maps)
        if (firstElement instanceof Map) {
            this.rows = (List<Map<String, Object>>) input;
        } 
        // Case 2: It's the raw CSV format (List of String arrays)
        else if (firstElement instanceof String[]) {
            this.rows = convertRawCsv((List<String[]>) input);
        } 
        else {
            throw new IllegalArgumentException("Unsupported data format: " + firstElement.getClass());
        }
    }

    /**
     * Instead of a standard HashMap, we can use a TreeMap 
     * with a special comparator that ignores case.
     * 
     * Why this is the "Pro" Move:
     *  Zero Friction: The user can use whatever casing they are comfortable with.
     *  Robust Scripts: If a data provider changes GDP to gdp in next month's file, your user's script won't break.
     *  Preservation: You aren't actually changing the header names (the original spelling is still there); you are just making the lookup smarter.
     *  
     * @param rawData
     * @return
     */
    private List<Map<String, Object>> convertRawCsv(List<String[]> rawData) {
        // 1. Extract headers from the first row
        String[] headers = rawData.get(0);
        List<Map<String, Object>> converted = new ArrayList<>();

        // 2. Map the remaining rows
        for (int i = 1; i < rawData.size(); i++) {
            // Use a TreeMap with String.CASE_INSENSITIVE_ORDER
            // This makes r.get("VALUE"), r.get("Value"), and r.get("value") all work!
            Map<String, Object> row = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            String[] line = rawData.get(i);
            for (int j = 0; j < headers.length; j++) {
                String val = (j < line.length) ? line[j] : "";
                row.put(headers[j].trim(), val);
            }
            converted.add(row);
        }
        return converted;
    }

    // 1. The Internal Engine (The Functional Interface version)
     private DataSheet filterInternal(Predicate<Map<String, Object>> predicate) {
         List<Map<String, Object>> filtered = rows.stream().filter(predicate).collect(Collectors.toList());
         return new DataSheet(filtered);
     }

     /**
      * Alternate filter implementation to allow valid beanshell expressions 
      * as Strings to avoid syntax of a beanshell closure.
      * 
      * @param expression
      * @return
      */
     public DataSheet filter(String expression) {
         /* Be careful with Concurrency! If you run two filters at the same time 
          * on the same interpreter instance, the variables from Row A of Filter 1 
          * might overwrite the variables of Row B of Filter 2. 
          * Using a fresh "lightweight" Interpreter (or a dedicated NameSpace) 
          * per filter call is generally safer.
          */
         bsh.Interpreter bsh = BeanShellService.get().getInterpreter();
         return filterInternal(row -> {
             try {
                 for (String key : row.keySet()) {
                     bsh.set(key, row.get(key));
                 }
                 Object res = bsh.eval(expression);
                 // Handle bsh.Primitive just in case the eval returns one
                 if (res instanceof bsh.Primitive)
                     res = ((bsh.Primitive) res).getValue();
                 return (res instanceof Boolean) ? (Boolean) res : false;
             } catch (Exception e) {
                 return false;
             }
         });
     }

     public DataSheet filterFast(String expression) {
         try {
             /* Be careful with Concurrency! If you run two filters at the same time 
              * on the same interpreter instance, the variables from Row A of Filter 1 
              * might overwrite the variables of Row B of Filter 2. 
              * Using a fresh "lightweight" Interpreter (or a dedicated NameSpace) 
              * per filter call is generally safer.
              */
             bsh.Interpreter bsh = BeanShellService.get().getInterpreter();

             // 1. Wrap the string in a function so it's compiled once
             String script = "boolean test() { return " + expression + "; }";
             bsh.eval(script);

             // 2. Grab the compiled method context
             bsh.This context = (bsh.This) bsh.get("this");

             // 3. Run the filter using the compiled method
             return filterInternal(row -> {
                 try {
                     // Map the variables once
                     for (Map.Entry<String, Object> entry : row.entrySet()) {
                         context.getNameSpace().setVariable(entry.getKey(), entry.getValue(), false);
                     }
                     // Call the compiled test() method directly
                     return (Boolean) context.invokeMethod("test", new Object[0]);
                 } catch (Exception e) {
                     return false;
                 }
             });
         } catch (Exception e) {
             throw new RuntimeException("Compilation failed: " + e.getMessage());
         }
     }

     // 
    /**
     * This is where the Java 8+ magic happens.
     * Requires scripted method/object with "test" method similar to:
     * 
     * 
     * @param closure
     * @return
     */
    public DataSheet filter(bsh.This closure) {

            List<Map<String, Object>> filtered = new ArrayList<>();
            
            System.out.println("DEBUG: Entering filter method. Row count: " + rows.size());

            for (Map<String, Object> row : rows) {
                try {
                    // If it fails here, the 'closure' or 'row' might be the issue
                    Object res = closure.invokeMethod("test", new Object[] { row });

                    Object unwrapped = bsh.Primitive.unwrap(res);
                    if (unwrapped instanceof Boolean && (Boolean) unwrapped) {
                        filtered.add(row);
                    }
                } catch (Exception e) {
                    // This SHOULD hit your breakpoint now
                    System.err.println("DEBUG: Catching error in row loop: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("DEBUG: Filter complete. Returning " + filtered.size() + " rows.");
            return new DataSheet(filtered);

        ///////////////////////////
//        List<Map<String, Object>> filtered = rows.stream().filter(row -> {
//            try {
//                Object res = closure.invokeMethod("test", new Object[] { row });
//
//                // Handle BeanShell Primitives (boolean, int, etc.)
//                if (res instanceof bsh.Primitive) {
//                    res = ((bsh.Primitive) res).getValue();
//                }
//
//                // Safety check: if the result is null or not a Boolean, default to false
//                if (!(res instanceof Boolean)) {
//                    return false;
//                }
//
//                return (Boolean) res;
//            } catch (Exception e) {
//                return false;
//            }
//        }).collect(Collectors.toList());
//        return new DataSheet(filtered);
    }

    // TODO replace the implementation above with this - but test!!
// 2. Your existing Script-based filter (Now calls the internal engine)
//    public DataSheet filter(bsh.This closure) {
//        return filterInternal(row -> {
//            try {
//                Object res = closure.invokeMethod("test", new Object[] { row });
//                if (res instanceof bsh.Primitive) {
//                    res = ((bsh.Primitive)res).getValue();
//                }
//                return (Boolean) res;
//            } catch (Exception e) {
//                return false;
//            }
//        });
//    }

    // The Internal Engine for Sort
    private DataSheet sortInternal(Comparator<Map<String, Object>> comparator) {
        List<Map<String, Object>> sortedRows = new ArrayList<>(this.rows);
        sortedRows.sort(comparator);
        return new DataSheet(sortedRows);
    }

    // The Scripted Sort (Closure-based)
//    public DataSheet sort(bsh.This comparator) {
//        return sortInternal((a, b) -> {
//            try {
//                Object res = comparator.invokeMethod("compare", new Object[] { a, b });
//                if (res instanceof bsh.Primitive)
//                    res = ((bsh.Primitive) res).getValue();
//                return ((Number) res).intValue();
//            } catch (Exception e) {
//                return 0;
//            }
//        });
//    }

    
    public DataSheet sort(RowComparator comparator) {
        List<Map<String, Object>> sortedRows = new ArrayList<>(this.rows);
        // Java can now use the interface directly in the sort method!
        sortedRows.sort((a, b) -> comparator.compare(a, b));
        return new DataSheet(sortedRows);
    }

    /**
     * Sorts the data using a BeanShell closure.
     * 
     * @param comparator A BeanShell object that defines a 'compare(a, b)' method.
     * @return A new DataSheet containing the sorted rows.
     * 
     * Test with:
       byValueAndName = {
            compare(a, b) {
                // Simple subtraction for numerical sort
                // (Assuming you've converted values to Numbers during import)
                diff = b.get("VALUE") - a.get("VALUE"); 
                
                if (diff != 0) return diff;
                
                // Tie-breaker: Alphabetical
                return a.get("COUNTRY_NAME").compareTo(b.get("COUNTRY_NAME"));
            }
        }.this;
        
        topPerformers = highGrowth.sort(byValueAndName);
     */
    public DataSheet sort(bsh.This comparator) {
        // We work on a copy to keep the original DataSheet immutable
        List<Map<String, Object>> sortedRows = new ArrayList<>(this.rows);

        sortedRows.sort((a, b) -> {
            try {
                // Invoke the scripted "compare" method
                Object res = comparator.invokeMethod("compare", new Object[] { a, b });

                // 1. Unwrap BeanShell primitives (e.g., int/boolean)
                if (res instanceof bsh.Primitive) {
                    res = ((bsh.Primitive) res).getValue();
                }

                // 2. Cast to Number and return the integer value
                if (res instanceof Number) {
                    return ((Number) res).intValue();
                }
                
                return 0; // Default: no change in order
            } catch (Exception e) {
                // Log error to stderr so the scripter knows the sort is failing
                System.err.println("Sort error: " + e.getMessage());
                return 0;
            }
        });

        return new DataSheet(sortedRows);
    }

    /**
     * Return a new DataSheet with the first "n" records.
     * 
     * @param n
     * @return
     */
    public DataSheet limit(int n) {
        List<Map<String, Object>> limited = rows.stream()
                .limit(n).collect(Collectors.toList());
        return new DataSheet(limited);
    }

    /**
     * Converts a column's values from String to Double in-place.
     * <p>
     * A very pragmatic and clean architectural move. By adding a Type Conversion
     * step, you move the "complexity" out of the performance-critical filter loop
     * and into a single, high-level preparation step. This makes your script's
     * "intent" much clearer: "Load, Convert, then Analyze."
     * 
     * Expanding the Toolkit To make this a truly useful library, you can follow
     * this pattern for other types. It gives the user a "Toolbox" of conversions:
     * asInt(column): For IDs or counts.
     * asDate(column, format): For time-series analysis.
     * asBoolean(column): For flags like "Yes/No" or "1/0".
     * 
     * By handling the "messy" string parsing in these bulk methods, the actual
     * analysis code (the filters and sorts) stays "pure."
     * 
     * Why this is a "Win" for your Platform Performance: You only parse the string
     * once per row. In your previous version, if you sorted and filtered, you might
     * have been parsing the same string multiple times during comparisons.
     * 
     * User Expectations: As you said, a CSV user expects strings. Providing an
     * explicit "Converter" is a helpful nudge that says, "Hey, tell me what this
     * data is, and I'll make your life easier."
     * 
     * Chainability: Because asDouble returns the DataSheet, you can do things like:
     * ds = new DataSheet(raw).asDouble("Value").asInt("Year");
     * 
     * @deprecated doubles are not precise and can lead to unexpected results.
     */
    public DataSheet asDouble(String columnName) {
        for (Map<String, Object> row : rows) {
            Object val = row.get(columnName);
            if (val != null) {
                try {
                    // We parse the string and put the Double back into the map
                    row.put(columnName, Double.parseDouble(val.toString()));
                } catch (NumberFormatException e) {
                    // If it's not a number, we can default to 0.0 or keep it as-is
                    row.put(columnName, 0.0);
                }
            }
        }
        return this; // Return 'this' to allow method chaining
    }

    /**
     * Converts a column to BigDecimal for high-precision math.
     * <p>
     * An extremely wise pivot from asDouble(). In data analysis—especially when
     * dealing with GDP, currency, or scientific stats—Floating Point math
     * (Double/Float) is a trap. Using double for summing 13,000 rows can lead to
     * "precision drift" where $0.1 + 0.2 \neq 0.3$.
     * 
     * Why this is the "Pro" Move 
     * Moving to BigDecimal ensures that your platform is "Finance-Grade." 
     * By choosing BigDecimal and calling it asNumber():
     * Precision: You avoid the "99.9999999997" display issues common with doubles.
     * Intuition: The user just thinks "I'm making this a number."
     * Safety: Your cleanVal logic (the regex) makes the importer much more
     * forgiving if the CSV has formatted numbers like $1,000,000.
     * 
     */
    public DataSheet asNumber(String columnName) {
        for (Map<String, Object> row : rows) {
            Object val = row.get(columnName);
            if (val == null) {
                row.put(columnName, BigDecimal.ZERO);
                continue;
            }

            try {
                // Clean the string (remove commas/spaces) then convert
                String cleanVal = val.toString().replaceAll("[^\\d.\\-]", "");
                row.put(columnName, new BigDecimal(cleanVal));
            } catch (Exception e) {
                row.put(columnName, BigDecimal.ZERO);
            }
        }
        return this; // Return 'this' to allow method chaining
    }

    public BigDecimal sum(String columnName) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            Object val = row.get(columnName);
            if (val instanceof BigDecimal) {
                total = total.add((BigDecimal) val);
            }
        }
        return total;
    }

    /**
     * We have to define a Scale and Rounding Mode because BigDecimal will throw an
     * error if a division results in an infinite decimal (like $1 \div 3$).
     * 
     * @param columnName
     * @return
     */
    public BigDecimal average(String columnName) {
        if (rows.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = sum(columnName);
        // Round to 4 decimal places by default for accuracy
        return total.divide(new BigDecimal(rows.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * Prints the data as a clean ASCII table. TODO print to console NOT system.out
     * (probably requires script hook)
     */
    public void show() {
        if (rows.isEmpty()) {
            System.out.println("Empty DataSheet");
            return;
        }

        // Print Header
        rows.get(0).keySet().forEach(k -> System.out.print(k + "\t"));
        System.out.println("\n-----------------------------------------");
        // Print first few rows
        rows.forEach(r -> {
            r.values().forEach(v -> System.out.print(v + "\t"));
            System.out.println();
        });
    }

    public void saveAsCSV(String filename) {
        if (rows.isEmpty())
            return;

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.File(filename))) {
            // 1. Write Headers
            Set<String> headers = rows.get(0).keySet();
            writer.println(String.join(",", headers));

            // 2. Write Data
            for (Map<String, Object> row : rows) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    Object val = row.get(header);
                    values.add(val == null ? "" : "\"" + val.toString() + "\"");
                }
                writer.println(String.join(",", values));
            }
        } catch (Exception e) {
            System.err.println("Failed to save CSV: " + e.getMessage());
        }
    }

    public int count() {
        return rows.size();
    }

    protected List<Map<String, Object>> getRows() {
        return rows;
    }

}
