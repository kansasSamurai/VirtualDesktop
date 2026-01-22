package org.jwellman.bsh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jwellman.virtualdesktop.bsh.BeanShellService; 

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

    private List<Map<String, Object>> convertRawCsv(List<String[]> rawData) {
        // 1. Extract headers from the first row
        String[] headers = rawData.get(0);
        List<Map<String, Object>> rows = new ArrayList<>();

        // 2. Map the remaining rows
        for (int i = 1; i < rawData.size(); i++) {
            String[] line = rawData.get(i);
            Map<String, Object> row = new HashMap<>();

            // Fill the map based on header names
            for (int j = 0; j < headers.length; j++) {
                // Check to avoid ArrayIndexOutOfBounds if a line is short
                String value = (j < line.length) ? line[j] : "";
                row.put(headers[j].trim(), value);
            }
            rows.add(row);
        }
        return rows;
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
     * Prints the data as a clean ASCII table.
     * TODO print to console NOT system.out (probably requires script hook)
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
