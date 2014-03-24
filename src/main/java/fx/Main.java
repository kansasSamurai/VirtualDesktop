/*
 XionDE.fm - XionDE File Manager
 Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

 This program can be distributed under the terms of the GNU GPL
 See the file COPYING.
 */
package fx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * File Manager - main class
 * 
 * @author Rick Wellman
 */
public class Main {
    
    // native library
    public static final String SHARED_LIB = "libfm.so";
    
    // multiplatform binary
    public static final String MPLATFORM_BIN = "fm.jar";
    
    // library paths
    public static final String NATIVE_PATH = "native/linux", LIB32_PATH = "lib", LIB64_PATH = "lib64";
    
    // native library prefix (for debug)
    public static final String NATIVE_PREFIX_DEF = "../../";
    public static String nativePrefix = "";
    
    // debug option
    public static final String DEBUG_FLAG = "--debug";
    
    // install (app) path
    public static String appPath;
    
    // library path
    public static String libPath;
    
    // configuration database
    public ConfigDb conf;
    
    /**
     * Init app
     * 
     * @param args 
     */
    public static void main(String[] args) {
        // check app path
        try {
            if (args != null) {
                if (args.length > 0) {
                    if (args[0].equals(DEBUG_FLAG)) {
                        nativePrefix = NATIVE_PREFIX_DEF;
                    }
                }
            }

            final String cp = "C:\\dev\\workspaces\\git\\VirtualDesktop\\target\\classes"; // System.getProperty("java.class.path");            
            File path = new File(cp); // full install (app) path = class path
            path = path.getCanonicalFile();
            if (path.exists()) // check path
            {
                // extract install (app) path
                appPath = path.getPath().substring(0, path.getPath().lastIndexOf(File.separator));
                File app = new File(appPath);
                if (!app.exists() || !app.isDirectory()) // check dir
                {
                    fx.Main.killApp("main", "invalid application path");
                }
            } else {
                fx.Main.killApp("main", "invalid path to application file");
            }
        } catch (Exception ex) {
            fx.Main.killApp("main", "cannot resolve application path");
        }
        // check native library
        try {
            final String strLib32 = appPath + File.separator + nativePrefix + NATIVE_PATH + File.separator + LIB32_PATH + File.separator + SHARED_LIB;
            final String strLib64 = appPath + File.separator + nativePrefix + NATIVE_PATH + File.separator + LIB64_PATH + File.separator + SHARED_LIB;
            File lib32 = new File(strLib32);
            File lib64 = new File(strLib64);

            // 64-bit
            if (System.getProperty("sun.arch.data.model").equals("64")) {
                if (lib64.exists()) // use 64-bit library
                {
                    libPath = appPath + File.separator + nativePrefix + NATIVE_PATH + File.separator + LIB64_PATH;
                } else // backing to 32-bit
                if (lib32.exists()) // use 32-bit library
                {
                    libPath = appPath + File.separator + nativePrefix + NATIVE_PATH + File.separator + LIB32_PATH;
                } else // no library
                {
                    fx.Main.killApp("main", "native library not found");
                }
            }
            // 32-bit
            if (System.getProperty("sun.arch.data.model").equals("32")) {
                if (lib32.exists()) // use 32-bit library
                {
                    libPath = appPath + File.separator + nativePrefix + NATIVE_PATH + File.separator + LIB32_PATH;
                } else // backing to 64-bit
                if (lib64.exists()) // use 64-bit library
                {
                    libPath = appPath + File.separator + nativePrefix + NATIVE_PATH + File.separator + LIB64_PATH;
                } else // no library
                {
                    fx.Main.killApp("main", "native library not found");
                }
            }
        } catch (Exception ex) {
            fx.Main.killApp("main", "cannot check native library");
        }
        // seems all ok - construct app
        Main main = new Main();
    }
    
    /**
     * Construct the app
     */
    public Main() {
        try {
            conf = new ConfigDb(); // create configuration database
            RootFrame root = new RootFrame(this); // create root frame
        } catch (Exception ex) {
            fx.Main.killApp("main", "cannot initialize gui");
        }
    }

    /**
     * The original implementation assumed Linux URLs;
     * adjust these to be windows compliant (and this assumes c: drive)
     * 
     * @param url
     * @return 
     */
    public static String adjustURL(String url) {
        String strurl = url.replace("jar:file:C:", "jar:file:/C:");
        strurl = strurl.replace("\\", "/");
        return strurl;
    }

    /**
     * shutdown app and show error message
     * 
     * @param module
     * @param message 
     */
    public static void killApp(String module, String message) {
        killApp(module, message, null);
    }
    
    public static void killApp(String module, String message, Exception e) {
        if (e != null) e.printStackTrace();
        System.err.println("Error in module \"" + module + "\": " + message);
        System.exit(-1);
    }
    
    // safely exit app and save settings

    public static void exitApp(Main main) {
        main.conf.saveConfig();
        System.exit(0);
    }
    // config database class

    public class ConfigDb {
        // config keys

        public static final String GLOB = "global.properties", GLOB_ROOT = "global", GLOB_ROOT_ICONS = GLOB_ROOT + ".icons",
                FM = "fm.properties", FM_ROOT = "fm", FM_ROOT_START = FM_ROOT + ".start",
                FM_ROOT_APPS = FM_ROOT + ".mime",
                FM_ROOT_FRM = FM_ROOT + ".root",
                FM_ROOT_FRM_MENU = FM_ROOT_FRM + ".menu", // main menu
                FM_ROOT_FRM_TOOL = FM_ROOT_FRM + ".toolbar", // tool bar (count required!)
                FM_ROOT_FRM_ADDR = FM_ROOT_FRM + ".address", // address bar
                FM_ROOT_FRM_FILE = FM_ROOT_FRM + ".manager", // file manager
                FM_ROOT_FRM_FILE_ITEMS = FM_ROOT_FRM_FILE + ".items", // fm items
                FM_ROOT_FRM_FILE_FRAMES = FM_ROOT_FRM_FILE + ".frames", // fm frames
                FM_ROOT_FRM_FILE_POPUP = FM_ROOT_FRM_FILE + ".popup", // fm popup menu
                FM_ROOT_FRM_STAT = FM_ROOT_FRM + ".status", // status bar
                COMP_WIDTH = ".width", COMP_HEIGHT = ".height", COMP_COUNT = ".count",
                COMP_ACTION = ".action", COMP_TEXT = ".text", COMP_SHORTCUT = ".shortcut";
        // configuration databases
        private Properties globDbDef = new Properties(), fxDbDef = new Properties(),
                globDb = new Properties(), fxDb = new Properties();
        // dirs
        private final String CONFIG_DIR = ".xionde", ICONS_DIR = "icons";
        public String iconThemeDir;
        // construct configuration database

        public ConfigDb() {
            // check dirs
            try {
                // check user's home dir
                File homeDir = new File(System.getProperty("user.home"));
                if (!homeDir.exists() || !homeDir.isDirectory()) // home dir
                {
                    fx.Main.killApp("config", "user's home directory doesn't exists");
                }
                // check config dir   
                File confDir = new File(homeDir.getPath() + File.separator + CONFIG_DIR);
                if (!confDir.exists() || !confDir.isDirectory()) // config dir
                {
                    if (!confDir.mkdir()) // cannot create config dir
                    {
                        fx.Main.killApp("config", "cannot create config directory");
                    }
                }
                // check icon themes dir
                File iconsDir = new File(fx.Main.appPath + File.separator + ICONS_DIR);
                if (!iconsDir.exists() || !iconsDir.isDirectory()) // icons dir
                {
                    fx.Main.killApp("config", "icon themes directory doesn't exists");
                } else {
                    iconThemeDir = iconsDir.getPath();
                }
                // load defaults
                loadDefaults();
                // load config
                loadConfig();
            } catch (Exception e) {
                fx.Main.killApp("config", "configuration database error");
            }
        }
        // load default database

        private void loadDefaults() {
            // set default icon theme (first file in themes dir)
            File iconsDir = new File(fx.Main.appPath + File.separator + ICONS_DIR);
            if (iconsDir.listFiles().length > 0) {
                globDbDef.setProperty(GLOB_ROOT_ICONS,
                        iconsDir.listFiles()[0].getName().substring(0,
                        iconsDir.listFiles()[0].getName().lastIndexOf(".")));
            } else {
                fx.Main.killApp("config", "icon theme not found");
            }
            // set default start dir (home dir)
            fxDbDef.setProperty(FM_ROOT_START, System.getProperty("user.home"));
            // setup gui
            fxDbDef.setProperty(FM_ROOT_FRM + COMP_WIDTH, "500");
            fxDbDef.setProperty(FM_ROOT_FRM + COMP_HEIGHT, "400");
            // main menu
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + COMP_COUNT, "6");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0" + COMP_TEXT, "File");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0" + COMP_COUNT, "10");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.0" + COMP_TEXT, "Open");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.0" + COMP_ACTION, RootFrame.ACTION_OPEN);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.0" + COMP_SHORTCUT, "ENTER");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.1" + COMP_TEXT, "Open with...");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.1" + COMP_ACTION, RootFrame.ACTION_OPENWITH);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.3" + COMP_TEXT, "New");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.3" + COMP_COUNT, "3");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.3.0" + COMP_TEXT, "Directory");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.3.0" + COMP_ACTION, RootFrame.ACTION_MKDIR);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.3.1" + COMP_TEXT, "Symlink (not implemented)");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.3.1" + COMP_ACTION, RootFrame.ACTION_MKLINK);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.3.2" + COMP_TEXT, "File... (not implemented)");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.3.2" + COMP_ACTION, RootFrame.ACTION_MKFILE);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.5" + COMP_TEXT, "Delete");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.5" + COMP_ACTION, RootFrame.ACTION_DELETE);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.5" + COMP_SHORTCUT, "DELETE");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.6" + COMP_TEXT, "Rename");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.6" + COMP_ACTION, RootFrame.ACTION_RENAME);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.6" + COMP_SHORTCUT, "F2");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.7" + COMP_TEXT, "Properties");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.7" + COMP_ACTION, RootFrame.ACTION_PROPERTIES);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.7" + COMP_SHORTCUT, "alt ENTER");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.9" + COMP_TEXT, "Exit");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".0.9" + COMP_ACTION, RootFrame.ACTION_EXIT);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1" + COMP_TEXT, "Edit");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1" + COMP_COUNT, "7");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.0" + COMP_TEXT, "Cut");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.0" + COMP_ACTION, RootFrame.ACTION_CUT);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.0" + COMP_SHORTCUT, "shift X");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.1" + COMP_TEXT, "Copy");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.1" + COMP_ACTION, RootFrame.ACTION_COPY);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.1" + COMP_SHORTCUT, "shift C");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.2" + COMP_TEXT, "Paste");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.2" + COMP_ACTION, RootFrame.ACTION_PASTE);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.2" + COMP_SHORTCUT, "shift V");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.3" + COMP_TEXT, "Paste symlink");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.3" + COMP_ACTION, RootFrame.ACTION_PASTELINK);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.5" + COMP_TEXT, "Select all");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.5" + COMP_ACTION, RootFrame.ACTION_SELALL);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.5" + COMP_SHORTCUT, "control A");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.6" + COMP_TEXT, "Invert selection");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".1.6" + COMP_ACTION, RootFrame.ACTION_INVSEL);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2" + COMP_TEXT, "View");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2" + COMP_COUNT, "9");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.0" + COMP_TEXT, "Tile");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.0" + COMP_ACTION, RootFrame.ACTION_VIEWTILE);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.1" + COMP_TEXT, "Icon");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.1" + COMP_ACTION, RootFrame.ACTION_VIEWICON);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.2" + COMP_TEXT, "List");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.2" + COMP_ACTION, RootFrame.ACTION_VIEWLIST);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.3" + COMP_TEXT, "Details");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.3" + COMP_ACTION, RootFrame.ACTION_VIEWDETAILS);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5" + COMP_TEXT, "Sort");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5" + COMP_COUNT, "4");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5.0" + COMP_TEXT, "Name");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5.0" + COMP_ACTION, RootFrame.ACTION_SORTNAME);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5.1" + COMP_TEXT, "Size");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5.1" + COMP_ACTION, RootFrame.ACTION_SORTSIZE);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5.2" + COMP_TEXT, "Type");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5.2" + COMP_ACTION, RootFrame.ACTION_SORTTYPE);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5.3" + COMP_TEXT, "Date");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.5.3" + COMP_ACTION, RootFrame.ACTION_SORTDATE);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7" + COMP_TEXT, "Go");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7" + COMP_COUNT, "3");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7.0" + COMP_TEXT, "Up");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7.0" + COMP_ACTION, RootFrame.ACTION_GOUP);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7.0" + COMP_SHORTCUT, "BACK_SPACE");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7.1" + COMP_TEXT, "Home directory");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7.1" + COMP_ACTION, RootFrame.ACTION_GOHOME);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7.1" + COMP_SHORTCUT, "alt HOME");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7.2" + COMP_TEXT, "Root directory");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.7.2" + COMP_ACTION, RootFrame.ACTION_GOROOT);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.8" + COMP_TEXT, "Refresh");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.8" + COMP_ACTION, RootFrame.ACTION_REFRESH);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".2.8" + COMP_SHORTCUT, "F5");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".3" + COMP_TEXT, "Bookmarks");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".3" + COMP_COUNT, "3");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".3.0" + COMP_TEXT, "Root directory - /");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".3.0" + COMP_ACTION, RootFrame.ACTION_BOOK + "/");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".3.1" + COMP_TEXT, "Mount-points - /mnt");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".3.1" + COMP_ACTION, RootFrame.ACTION_BOOK + "/mnt");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".3.2" + COMP_TEXT, "Users directory - /home");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".3.2" + COMP_ACTION, RootFrame.ACTION_BOOK + "/home");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4" + COMP_TEXT, "Tools");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4" + COMP_COUNT, "5");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4.0" + COMP_TEXT, "Mount directory");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4.0" + COMP_ACTION, RootFrame.ACTION_MOUNT);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4.1" + COMP_TEXT, "Unmount directory");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4.1" + COMP_ACTION, RootFrame.ACTION_UNMOUNT);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4.2" + COMP_TEXT, "Eject directory");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4.2" + COMP_ACTION, RootFrame.ACTION_EJECT);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4.4" + COMP_TEXT, "Options...");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".4.4" + COMP_ACTION, RootFrame.ACTION_OPTIONS);
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".5" + COMP_TEXT, "Help");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".5" + COMP_COUNT, "1");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".5.0" + COMP_TEXT, "About");
            fxDbDef.setProperty(FM_ROOT_FRM_MENU + ".5.0" + COMP_ACTION, RootFrame.ACTION_ABOUT);
            // tool bar
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + COMP_COUNT, "14");
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".0" + COMP_ACTION, RootFrame.ACTION_GOUP);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".1" + COMP_ACTION, RootFrame.ACTION_REFRESH);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".2" + COMP_ACTION, RootFrame.ACTION_GOHOME);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".4" + COMP_ACTION, RootFrame.ACTION_CUT);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".5" + COMP_ACTION, RootFrame.ACTION_COPY);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".6" + COMP_ACTION, RootFrame.ACTION_PASTE);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".8" + COMP_ACTION, RootFrame.ACTION_DELETE);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".10" + COMP_ACTION, RootFrame.ACTION_VIEWTILE);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".11" + COMP_ACTION, RootFrame.ACTION_VIEWICON);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".12" + COMP_ACTION, RootFrame.ACTION_VIEWLIST);
            fxDbDef.setProperty(FM_ROOT_FRM_TOOL + ".13" + COMP_ACTION, RootFrame.ACTION_VIEWDETAILS);
            // address bar - address label
            fxDbDef.setProperty(FM_ROOT_FRM_ADDR + ".1" + COMP_TEXT, "Address:");
            // address bar - address button
            fxDbDef.setProperty(FM_ROOT_FRM_ADDR + ".2" + COMP_TEXT, "Go");
            // file manager
            fxDbDef.setProperty(FM_ROOT_FRM_FILE + ".1" + COMP_ACTION,
                    RootFrame.ACTION_VIEWTILE); // view style
            fxDbDef.setProperty(FM_ROOT_FRM_FILE + ".2" + COMP_ACTION,
                    RootFrame.ACTION_SORTNAME); // sort type
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_ITEMS + ".1" + COMP_TEXT,
                    RootFrame.VIEW_MODE); // comment 1 (in tile view)
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_ITEMS + ".2" + COMP_TEXT,
                    RootFrame.VIEW_SIZE); // comment 2 (in tile view)
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_ITEMS + ".3" + COMP_TEXT,
                    RootFrame.VIEW_NAME + "#" + 200 + "#"
                    + RootFrame.VIEW_SIZE + "#" + 75 + "#"
                    + RootFrame.VIEW_TYPE + "#" + 120 + "#"
                    + RootFrame.VIEW_DATE + "#" + 200); // details (in details view)
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_ITEMS + ".4" + COMP_TEXT,
                    "<html>Name: " + RootFrame.VIEW_NAME
                    + "<br>Size: " + RootFrame.VIEW_SIZE
                    + "<br>Date: " + RootFrame.VIEW_DATE
                    + "<br>UID/GID: " + RootFrame.VIEW_UID + "/" + RootFrame.VIEW_GID
                    + "<br>Owner/Group: " + RootFrame.VIEW_OWNER + "/" + RootFrame.VIEW_GROUP
                    + "<br>Mode: " + RootFrame.VIEW_MODE
                    + "<br>Mime: " + RootFrame.VIEW_TYPE
                    + "<br>Description: " + RootFrame.VIEW_INFO
                    + "</html>"); // tool tip
            // copy frame options (info, error, question)
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".1.1" + COMP_TEXT, "Copying...#File#Cancel");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".1.2" + COMP_TEXT, "Error#Can't copy file");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".1.3" + COMP_TEXT, "Question#Rewrite file#Yes#Yes to all#No#No to all#Cancel");
            // delete frame options (info, error, question)
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".2.1" + COMP_TEXT, "Deleting...#File#Cancel");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".2.2" + COMP_TEXT, "Error#Can't delete file");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".2.3" + COMP_TEXT, "Question#Delete file");
            // action errors (open, create dir, create file, rename, mount, unmount, eject)
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".3.2" + COMP_TEXT, "Error#Can't open directory");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".4.2" + COMP_TEXT, "Error#Can't create directory");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".4.3" + COMP_TEXT, "Input#Enter new name of directory");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".5.2" + COMP_TEXT, "Error#Can't create file");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".5.3" + COMP_TEXT, "Choice#Select type of file to create");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".6.2" + COMP_TEXT, "Error#Can't rename file");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".6.3" + COMP_TEXT, "Input#Enter new name of file");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".7.2" + COMP_TEXT, "Error#Can't mount directory");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".8.2" + COMP_TEXT, "Error#Can't unmount directory");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_FRAMES + ".9.2" + COMP_TEXT, "Error#Can't eject directory");
            // popup menu
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + COMP_COUNT, "13");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".0" + COMP_TEXT, "Open");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".0" + COMP_ACTION, RootFrame.ACTION_OPEN);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".0" + COMP_SHORTCUT, "ENTER");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".1" + COMP_TEXT, "Open with...");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".1" + COMP_ACTION, RootFrame.ACTION_OPENWITH);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".3" + COMP_TEXT, "New");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".3" + COMP_COUNT, "3");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".3.0" + COMP_TEXT, "Directory");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".3.0" + COMP_ACTION, RootFrame.ACTION_MKDIR);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".3.1" + COMP_TEXT, "Symlink (not implemented)");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".3.1" + COMP_ACTION, RootFrame.ACTION_MKLINK);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".3.2" + COMP_TEXT, "File... (not implemented)");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".3.2" + COMP_ACTION, RootFrame.ACTION_MKFILE);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".5" + COMP_TEXT, "Cut");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".5" + COMP_ACTION, RootFrame.ACTION_CUT);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".5" + COMP_SHORTCUT, "shift X");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".6" + COMP_TEXT, "Copy");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".6" + COMP_ACTION, RootFrame.ACTION_COPY);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".6" + COMP_SHORTCUT, "shift C");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".7" + COMP_TEXT, "Paste");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".7" + COMP_ACTION, RootFrame.ACTION_PASTE);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".7" + COMP_SHORTCUT, "shift V");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".9" + COMP_TEXT, "Delete");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".9" + COMP_ACTION, RootFrame.ACTION_DELETE);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".9" + COMP_SHORTCUT, "DELETE");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".10" + COMP_TEXT, "Rename");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".10" + COMP_ACTION, RootFrame.ACTION_RENAME);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".10" + COMP_SHORTCUT, "F2");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".12" + COMP_TEXT, "Properties");
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".12" + COMP_ACTION, RootFrame.ACTION_PROPERTIES);
            fxDbDef.setProperty(FM_ROOT_FRM_FILE_POPUP + ".12" + COMP_SHORTCUT, "alt ENTER");

            // status bar - label 1
            // single file selected mode
            fxDbDef.setProperty(FM_ROOT_FRM_STAT + ".1.1" + COMP_TEXT,
                    "Date: " + RootFrame.VIEW_DATE + " Type: " + RootFrame.VIEW_TYPE);
            // multiple file selected mode
            fxDbDef.setProperty(FM_ROOT_FRM_STAT + ".1.2" + COMP_TEXT, "Selected objects:");
            // without selection mode (idle)
            fxDbDef.setProperty(FM_ROOT_FRM_STAT + ".1.3" + COMP_TEXT, "Objects:");
            // loading mode
            fxDbDef.setProperty(FM_ROOT_FRM_STAT + ".1.4" + COMP_TEXT, "Loading:");
            // status bar - label 2
            fxDbDef.setProperty(FM_ROOT_FRM_STAT + ".2" + COMP_TEXT, "Size:");
            // status bar - label 3
            fxDbDef.setProperty(FM_ROOT_FRM_STAT + ".3" + COMP_TEXT, "Free:");
        }
        // load database from properties file

        public void loadConfig() {
            FileInputStream in;
            try {
                // setup config files
                File globConf = new File(System.getProperty("user.home") + File.separator + CONFIG_DIR + File.separator + GLOB);
                File fxConf = new File(System.getProperty("user.home") + File.separator + CONFIG_DIR + File.separator + FM);
                // load global configuration database
                if (globConf.exists()) {
                    in = new FileInputStream(globConf);
                    globDb.load(in);
                    in.close();
                }
                // load fx configuration database
                if (fxConf.exists()) {
                    in = new FileInputStream(fxConf);
                    fxDb.load(in);
                    in.close();
                }
            } catch (Exception e) {
                killApp("config", "config file loading error");
            }
        }
        // save database to properties file

        public void saveConfig() {
            FileOutputStream out;
            try {
                // save global configuration database
                out = new FileOutputStream(System.getProperty("user.home")
                        + File.separator + CONFIG_DIR + File.separator + GLOB);
                globDb.store(out, "XionDE global config file");
                out.close();
                // save fx configuration database
                out = new FileOutputStream(System.getProperty("user.home")
                        + File.separator + CONFIG_DIR + File.separator + FM);
                fxDb.store(out, "XionDE.fm local config file");
                out.close();
            } catch (Exception e) {
                killApp("config", "config file saving error");
            }
        }
        // get property

        public String getProperty(String key) {
            if (key.indexOf(".") != -1) {
                if (key.substring(0, key.indexOf(".")).equals(GLOB_ROOT)) {
                    return globDb.getProperty(key, globDbDef.getProperty(key));
                }
                if (key.substring(0, key.indexOf(".")).equals(FM_ROOT)) {
                    return fxDb.getProperty(key, fxDbDef.getProperty(key));
                }
            }
            return null;
        }
        // set property

        public void setProperty(String key, String value) {
            if (key.indexOf(".") != -1) {
                if (key.substring(0, key.indexOf(".")).equals(GLOB_ROOT)) {
                    globDb.setProperty(key, value);
                }
                if (key.substring(0, key.indexOf(".")).equals(FM_ROOT)) {
                    fxDb.setProperty(key, value);
                }
            }
        }
        // remove property

        public void removeProperty(String key) {
            if (key.indexOf(".") != -1) {
                if (key.substring(0, key.indexOf(".")).equals(GLOB_ROOT)) {
                    globDb.remove(key);
                }
                if (key.substring(0, key.indexOf(".")).equals(FM_ROOT)) {
                    fxDb.remove(key);
                }
            }
        }
    }
}
