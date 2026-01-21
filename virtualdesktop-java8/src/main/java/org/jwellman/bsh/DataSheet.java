package org.jwellman.bsh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jwellman.virtualdesktop.bsh.BeanShellService; 

/**
 * 
 * @author rwellman
 *
 */
public class DataSheet {

    private List<Map<String, Object>> rows;

    public DataSheet(List<Map<String, Object>> rows) {
        this.rows = rows;
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
        List<Map<String, Object>> filtered = rows.stream().filter(row -> {
            try {
                Object res = closure.invokeMethod("test", new Object[] { row });

                // Handle BeanShell Primitives (boolean, int, etc.)
                if (res instanceof bsh.Primitive) {
                    res = ((bsh.Primitive) res).getValue();
                }

                // Safety check: if the result is null or not a Boolean, default to false
                if (!(res instanceof Boolean)) {
                    return false;
                }

                return (Boolean) res;
            } catch (Exception e) {
                return false;
            }
        }).collect(Collectors.toList());
        return new DataSheet(filtered);
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
    public DataSheet sort(bsh.This comparator) {
        return sortInternal((a, b) -> {
            try {
                Object res = comparator.invokeMethod("compare", new Object[] { a, b });
                if (res instanceof bsh.Primitive)
                    res = ((bsh.Primitive) res).getValue();
                return ((Number) res).intValue();
            } catch (Exception e) {
                return 0;
            }
        });
    }

    public int count() {
        return rows.size();
    }

    // You can even add a Bridge to your View object here
    public void show() {
        /* Call your app's Grid View */ 
    }

}
