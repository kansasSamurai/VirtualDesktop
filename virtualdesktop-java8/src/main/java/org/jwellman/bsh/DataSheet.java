package org.jwellman.bsh;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // This is where the Java 8+ magic happens
    public DataSheet filter(bsh.This closure) {
        List<Map<String, Object>> filtered = rows.stream().filter(row -> {
            try {
                return (Boolean) closure.invokeMethod("test", new Object[] { row });
            } catch (Exception e) {
                return false;
            }
        }).collect(Collectors.toList());
        return new DataSheet(filtered);
    }

    public int count() {
        return rows.size();
    }

    // You can even add a Bridge to your View object here
    public void show() {
        /* Call your app's Grid View */ 
    }

}
