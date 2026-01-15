/*
 XionDE.fm - XionDE File Manager
 Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

 This program can be distributed under the terms of the GNU GPL
 See the file COPYING.
 */
package fx.filemanager;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Vector;
import javax.swing.ImageIcon;

/**
 * icon theme class - provide icons
 * 
 * @author Rick Wellman
 */
public class IconTheme {
    
    // main values
    private final String 
            THEME_EXT = "jar", THEME_PREF = "jar:file:", THEME_SUF = "!",
            THEME_INDEX = "theme", ICON_EXT = "png",
            PACK_MAIN = "main", PACK_DEVICE = "device", PACK_MIME = "mime";
    
    // device icons mime db
    private final String[][] DEV_DB = {
        {"video", "removable/videocam"},
        {"phone", "removable/phone"},
        {"cell", "removable/phone"},
        {"mobile", "removable/phone"},
        {"pda", "removable/pda"},
        {"palm", "removable/pda"},
        {"pocket", "removable/pda"},
        {"camera", "removable/camera"},
        {"photo", "removable/camera"},
        {"player", "removable/player"},
        {"music", "removable/player"},
        {"cf", "removable/card-cf"},
        {"ms", "removable/card-ms"},
        {"sd", "removable/card-sdmmc"},
        {"mmc", "removable/card-sdmmc"},
        {"sm", "removable/card-sm"},
        {"xd", "removable/card-xd"},
        {"nfs", "network/nfs"},
        {"smb", "network/smb"},
        {"samba", "network/smb"},
        {"fd", "floppy/floppy"},
        {"floppy", "floppy/floppy"},
        {"cd", "cdrom/cd"},
        {"dvd", "cdrom/dvd"}};
    // theme file
    private String iconThemeFile;
    // default icon
    private ImageIcon defIcon;
    // icons cache
    private Vector iconsCache = new Vector();
    // icons cache map
    private Properties iconsCacheMap = new Properties();
    // icons hash map (from 0.2.1.1 we don't store files, just hash of unique files)
    private Properties iconsHashMap = new Properties();
    // fixed main icons
    public static final String MAIN_DEFA = "default", MAIN_MNTD = "mounted", MAIN_LINK = "symlink",
            MAIN_DCUT = "cut", MAIN_COPY = "copy", MAIN_PAST = "paste",
            MAIN_CPTO = "copyto", MAIN_MVTO = "moveto", MAIN_DELE = "delete",
            MAIN_PROP = "properties", MAIN_GOUP = "up", MAIN_HOME = "home",
            MAIN_REFR = "refresh", MAIN_DMNT = "mount", MAIN_UMNT = "unmount",
            MAIN_EJCT = "eject", MAIN_TILE = "tile", MAIN_ICON = "icon",
            MAIN_LIST = "list", MAIN_DETA = "details";

    /**
     * create icon theme
     * 
     * @param iconThemesDir
     * @param theme 
     */
    public IconTheme(String iconThemesDir, String theme) {
        try {
            final String strThemes = iconThemesDir + File.separator + theme + "." + THEME_EXT;
            File itheme = new File(strThemes);
            File ithemedir = new File(iconThemesDir);
            if (!itheme.exists()) {
                if (ithemedir.exists()) {
                    if (ithemedir.listFiles().length > 0) {
                        if (ithemedir.listFiles()[0].exists()) {
                            theme = ithemedir.listFiles()[0].getName().substring(0,
                                    ithemedir.listFiles()[0].getName().lastIndexOf("."));
                        }
                    }
                }
            }
            iconThemeFile = THEME_PREF + iconThemesDir + File.separator + theme + "." + THEME_EXT + THEME_SUF;
            final String urlTheme = fx.Main.adjustURL(iconThemeFile + File.separator + THEME_INDEX);
            final URL url = new URL(urlTheme);
            final JarURLConnection juc = (JarURLConnection) url.openConnection();
            final InputStream indexStream = juc.getInputStream();
            iconsHashMap.load(indexStream);
            
            final String prop = PACK_MAIN + "-" + MAIN_DEFA + "." + ICON_EXT;
            final String value = PACK_MAIN + "-" + MAIN_DEFA + "." + ICON_EXT;
            iconsHashMap.setProperty(prop, value);
            
            defIcon = loadIcon(PACK_MAIN, MAIN_DEFA);
            if (defIcon == null) {
                fx.Main.killApp("icons", "cannot load default icon");
            }
        } catch (Exception ex) {
            fx.Main.killApp("icons", "cannot open icon theme");
        }
    }
    // get registered devices list

    public String[][] getDevicesList() {
        return DEV_DB;
    }
    // get default icon

    public ImageIcon getDefIcon() {
        return defIcon;
    }
    // get mounted icon

    public ImageIcon getMntIcon() {
        return loadIconX(PACK_MAIN, MAIN_MNTD);
    }
    // get symlink icon

    public ImageIcon getLnkIcon() {
        return loadIconX(PACK_MAIN, MAIN_LINK);
    }
    // get icon

    public ImageIcon getIcon(String name, String mime) {
        // get icon by type
        if (mime == null) {
            return loadIconX(PACK_MAIN, name); // action
        }
        if (mime.equals("inode/mount-point")) // mountpoint
        {
            return getIconByMime(PACK_DEVICE, nameToMime(name));
        }
        return getIconByMime(PACK_MIME, mime); // mimetype
    }
    // get mime from name (only for mountpoints)

    private String nameToMime(String name) {
        // search name in db
        for (int i = 0; i < DEV_DB.length; i++) {
            if (name.contains(DEV_DB[i][0]))//.subSequence(0, DEV_DB[i][0].length())))
            {
                return DEV_DB[i][1];
            }
        }
        return "inode/mount-point"; // if not found
    }
    // get icon by mime (for regular file and devices))

    private ImageIcon getIconByMime(String pack, String mime) {
        if (mime.equals("")) { mime = "removable/phone"; } // TODO; fix this hack

        // get mime icons (mime info: superclass/subclass)
        // in icon filename we should replace "/" with the ":" for compatibility
        // load subclass mime icon        
        if (loadIcon(pack, mime.replace("/", ":")) != null) {
            return loadIcon(pack, mime.replace("/", ":"));
        } else // subclass not found - load superclass mime icon
        if (loadIcon(pack, mime.substring(0, mime.indexOf("/"))) != null) {
            return loadIcon(pack, mime.substring(0, mime.indexOf("/")));
        } else // superclass not found - get default icon in package
        if (pack.equals(PACK_DEVICE)) { // device icon
            return loadIconX(PACK_DEVICE, MAIN_DEFA); // default device icon
        } else {
            return getDefIcon(); // default icon
        }
    }
    // load icon from theme file

    private ImageIcon loadIcon(String pack, String name) {
        try {
            // properties bugfix: properties do not understand ":" incorrectly
            // so replace all ":" with "@"
            final String x = pack + "-" + name.replace(":", "@") + "." + ICON_EXT;
            if (iconsHashMap.getProperty(x, null) == null) {
                return null;
            }
            
            final String strurl = fx.Main.adjustURL(iconThemeFile + File.separator + iconsHashMap.getProperty(pack + "-" + name.replace(":", "@") + "." + ICON_EXT, PACK_MAIN + "-" + MAIN_DEFA + "." + ICON_EXT));
            final URL url = new URL(strurl);
            final JarURLConnection juc = (JarURLConnection) url.openConnection();
            juc.getJarFile();
            if (iconsCacheMap.getProperty(pack + "-" + name + "." + ICON_EXT) == null) {
                iconsCache.addElement(new ImageIcon(url));
                iconsCacheMap.setProperty(pack + "-" + name + "." + ICON_EXT, String.valueOf(iconsCache.size() - 1));
            }
            return (ImageIcon) iconsCache.elementAt(Integer.parseInt(iconsCacheMap.getProperty(pack + "-" + name + "." + ICON_EXT)));
        } catch (Exception e) {
            return null;
        }
    }
    // try to load icon, if not found get default

    private ImageIcon loadIconX(String pack, String name) {
        if (loadIcon(pack, name) != null) {
            return loadIcon(pack, name);
        } else {
            return getDefIcon();
        }
    }
}
