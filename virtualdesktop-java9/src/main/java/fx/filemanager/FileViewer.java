/*
 XionDE.fm - XionDE File Manager
 Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

 This program can be distributed under the terms of the GNU GPL
 See the file COPYING.
 */

package fx.filemanager;

import fx.filemanager.items.*;
import javax.swing.*;

/**
 * file viewer class
 * 
 * @author Rick Wellman
 */
public class FileViewer extends JList {
    // system value (i don't know what is it, but it is required) ???
    //public static final long serialVersionUID = 1;
    
    // info macros
    public static final String INFO_NAME = "$NAME", // name macros is available only in tool tip!
            INFO_SIZE = "$SIZE", INFO_DATE = "$DATE", INFO_UID = "$UID",
            INFO_OWNER = "$OWNER", INFO_GID = "$GID", INFO_GROUP = "$GROUP",
            INFO_MODE = "$MODE", INFO_MIME = "$MIME", INFO_DESC = "$DESC";
   
    // view styles and sort types possible options
    public static final int VIEW_TILE = 0, VIEW_ICON = 1, VIEW_LIST = 2, VIEW_DETAILS = 3,
            SORT_NAME = 0, SORT_SIZE = 1, SORT_TYPE = 2, SORT_DATE = 3;
    
    // options
    private int viewStyle, sortType; // current view style, sort type options
    private String cwd, // current working directory
            comment1Option = INFO_MODE, // comment 1 option (default - mode)
            comment2Option = INFO_SIZE, // comment 2 option (default - size)
            toolTipOption =
            "<html>Name: " + INFO_NAME
            + "<br>Size: " + INFO_SIZE
            + "<br>Date: " + INFO_DATE
            + "<br>UID/GID: " + INFO_UID + "/" + INFO_GID
            + "<br>Owner/Group: " + INFO_OWNER + "/" + INFO_GROUP
            + "<br>Mode: " + INFO_MODE
            + "<br>Mime: " + INFO_MIME
            + "<br>Description: " + INFO_DESC
            + "</html>", // tool tip option (set defaults)
            detailsOption =
            INFO_NAME + "#" + 200 + "#"
            + INFO_SIZE + "#" + 75 + "#"
            + INFO_MIME + "#" + 120 + "#"
            + INFO_DATE + "#" + 200, // details option -
            // synopsis: <column 1 data>#<column 1 size>#<column 2 data>#<column 2 size> ...
            loadingOption = "Loading:",
            idleOption = "Objects:",
            sizeOption = "Size:",
            accessError = "Error#Access denied";
    private boolean inited = false; // list is not initialised
    private IconTheme iconTheme;
    private JLabel progressLabel, sizeLabel; // get progress of scanning dirs, get size of all files
    private ScanThread scan = null; // scan dirs
    // construct viewer with specified icon theme

    public FileViewer(String startDir, IconTheme theme, JLabel prefProgressLabel, JLabel prefSizeLabel) {
        progressLabel = prefProgressLabel; // set pregress monitor
        sizeLabel = prefSizeLabel; // set size field
        setVisibleRowCount(0); // for auto configuring items
        setDoubleBuffered(true); // for better look and feel (perfomance ++)
        setDragEnabled(true); // you can drag files from file viewer
        setIconTheme(theme); // setup icon theme
        setViewStyle(VIEW_TILE); // set default view style
        setSortType(SORT_NAME); // set default sort type
        // set default cwd, but don't show it
        try {
            java.io.File startFile = new java.io.File(startDir);
            if (startFile.exists()) {
                cwd = startDir;
            } else {
                cwd = System.getProperty("user.home");
            }
        } catch (Exception ex) {
            cwd = System.getProperty("user.home");
        }
    }
    // initialize viewer (after configuration - view style, sort type)

    public void initialize() {
        inited = true;
    }
    // view style options

    public int getViewStyle() {
        return viewStyle;
    }

    public void setViewStyle(int style) {
        switch (style) {
            default: // set view style to tile (VIEW_TILE - default)
                viewStyle = VIEW_TILE;
                setCellRenderer(new TileItem());
                setLayoutOrientation(HORIZONTAL_WRAP);
                break;
            case VIEW_ICON: // set view style to icon
                viewStyle = VIEW_ICON;
                setCellRenderer(new IconItem());
                setLayoutOrientation(HORIZONTAL_WRAP);
                break;
            case VIEW_LIST: // set view style to list
                viewStyle = VIEW_LIST;
                setCellRenderer(new ListItem());
                setLayoutOrientation(VERTICAL_WRAP);
                break;
            case VIEW_DETAILS: // set view style to details
                viewStyle = VIEW_DETAILS;
                setCellRenderer(new DetailsItem());
                setLayoutOrientation(VERTICAL);
                break;
        }
        //if (inited) setCwd(getCwd()); // refresh
    }
    // sort type options

    public int getSortType() {
        return sortType;
    }

    public void setSortType(int type) {
        switch (type) {
            default: // set sort type to name (SORT_NAME - default)
                sortType = SORT_NAME;
                break;
            case SORT_SIZE: // set sort type to size
                sortType = SORT_SIZE;
                break;
            case SORT_TYPE: // set sort type to type
                sortType = SORT_TYPE;
                break;
            case SORT_DATE: // set sort type to date
                sortType = SORT_DATE;
                break;
        }
        if (inited) {
            setCwd(getCwd()); // refresh
        }
    }
    // comments options (comment 1,2 for line 2,3 in tile view)

    public String getComment1Option() {
        return comment1Option;
    }

    public String getComment2Option() {
        return comment2Option;
    }

    public void setComment1Option(String option) {
        comment1Option = option;
    }

    public void setComment2Option(String option) {
        comment2Option = option;
    }
    // details options

    public String getDetailsOption() {
        return detailsOption;
    }

    public void setDetailsOption(String option) {
        detailsOption = option;
    }
    // tool tip text

    public String getToolTipOption() {
        return toolTipOption;
    }

    public void setToolTipOption(String option) {
        toolTipOption = option;
    }
    // loading text

    public String getLoadingOption() {
        return loadingOption;
    }

    public void setLoadingOption(String option) {
        loadingOption = option;
    }
    // idle text (after loading)

    public String getIdleOption() {
        return idleOption;
    }

    public void setIdleOption(String option) {
        idleOption = option;
    }
    // size field text

    public String getSizeOption() {
        return sizeOption;
    }

    public void setSizeOption(String option) {
        sizeOption = option;
    }
    // access option (access denied message)

    public String getAccessError() {
        return accessError;
    }

    public void setAccessError(String option) {
        accessError = option;
    }
    // get/set icon theme

    public IconTheme getIconTheme() {
        return iconTheme;
    }

    public void setIconTheme(IconTheme theme) {
        iconTheme = theme;
    }
    // get/set cwd (current working directory)

    public String getCwd() {
        return cwd;
    }

    public void setCwd(String dir) {
        try {
            UnixFile newDir = new UnixFile(dir);
            newDir = newDir.getCanonicalFile();
            if (newDir.exists() && newDir.isDirectory()) {
                if (newDir.listFiles() != null) {
                    cwd = newDir.getPath();
                    if (scan != null) {
                        scan.kill();
                    }
                    scan = new ScanThread(this, progressLabel, sizeLabel);
                    scan.start();
                } else {
                    JOptionPane.showMessageDialog(null, getAccessError().split("#")[1],
                            getAccessError().split("#")[0], JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
        }
    }
}
