package org.jwellman.swing.grid;

public class ColumnDef {

    private final String key;
    private final String header;
    private final int preferredWidth;
    private final boolean sortable;
    private final boolean resizable;
    private final String fndType;

    public ColumnDef(String key, String header, int preferredWidth,
                     boolean sortable, boolean resizable, String fndType) {
        this.key = key;
        this.header = header;
        this.preferredWidth = preferredWidth;
        this.sortable = sortable;
        this.resizable = resizable;
        this.fndType = fndType;
    }

    /** Convenience constructor with sensible defaults. */
    public ColumnDef(String key, String header) {
        this(key, header, 100, false, true, null);
    }

    /** Convenience constructor for when sortability needs to be specified explicitly. */
    public ColumnDef(String key, String header, boolean sortable) {
        this(key, header, 100, sortable, true, null);
    }

    public String getKey() { return key; }
    public String getHeader() { return header; }
    public int getPreferredWidth() { return preferredWidth; }
    public boolean isSortable() { return sortable; }
    public boolean isResizable() { return resizable; }
    public String getFndType() { return fndType; }
}
