/*
 XionDE.fm - XionDE File Manager
 Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

 This program can be distributed under the terms of the GNU GPL
 See the file COPYING.
 */
// package File Manager
package fx;
// import classes
import java.awt.*; // for addititional purposes
import java.awt.dnd.*; // for drag & drop
import java.awt.datatransfer.*; // for drag & drop
import java.awt.event.*; // for events
import java.io.File; // file listing
import java.net.URI; // file locating for drag & drop
import java.util.Date; // file's date
import java.util.Properties; // for storing actions to icons map
import javax.swing.*; // for swing gui
import javax.swing.event.*; // for extended events
import fx.filemanager.*; // file manager package
import fx.filemanager.frames.OptionsFrame; // options
// File eXplorer - main frame class
public class RootFrame extends JFrame implements ActionListener {
    // main class

    private Main main;
    // icon theme
    private IconTheme iconTheme;
    // data mappings
    private Properties actionIcons = new Properties(); // actions to icons mapping
    private Properties viewStyles = new Properties(); // view actions to styles
    private Properties sortTypes = new Properties(); // sort actions to types
    // action, view macros
    public static final String // file menu
            ACTION_OPEN = "$OPEN", ACTION_OPENWITH = "$OPENWITH", ACTION_MKDIR = "$MKDIR",
            ACTION_MKLINK = "$MKLINK", ACTION_MKFILE = "$MKFILE", ACTION_DELETE = "$DELETE",
            ACTION_RENAME = "$RENAME", ACTION_PROPERTIES = "$PROPERTIES", ACTION_EXIT = "$EXIT",
            // edit menu
            ACTION_CUT = "$CUT", ACTION_COPY = "$COPY", ACTION_PASTE = "$PASTE",
            ACTION_PASTELINK = "$PASTELINK", ACTION_COPYTO = "$COPYTO",
            ACTION_MOVETO = "$MOVETO", ACTION_SELALL = "$SELALL", ACTION_INVSEL = "$INVSEL",
            // view menu
            ACTION_VIEWTILE = "$VIEWTILE", ACTION_VIEWICON = "$VIEWICON",
            ACTION_VIEWLIST = "$VIEWLIST", ACTION_VIEWDETAILS = "$VIEWDETAILS",
            ACTION_SORTNAME = "$SORTNAME", ACTION_SORTSIZE = "$SORTSIZE",
            ACTION_SORTTYPE = "$SORTTYPE", ACTION_SORTDATE = "$SORTDATE",
            ACTION_GOUP = "$GOUP", ACTION_GOHOME = "$GOHOME",
            ACTION_GOROOT = "$GOROOT", ACTION_REFRESH = "$REFRESH",
            // bookmarks
            ACTION_BOOK = "$BOOK",
            // tool menu
            ACTION_MOUNT = "$MOUNT", ACTION_UNMOUNT = "$UNMOUNT", ACTION_EJECT = "$EJECT",
            ACTION_OPTIONS = "$OPTIONS",
            // help menu
            ACTION_HELP = "$HELP", ACTION_ABOUT = "$ABOUT",
            // address (button, edit) action
            ACTION_GO = "$GO",
            // view macros
            VIEW_NAME = FileManager.INFO_NAME, VIEW_SIZE = FileManager.INFO_SIZE,
            VIEW_DATE = FileManager.INFO_DATE, VIEW_UID = FileManager.INFO_UID,
            VIEW_OWNER = FileManager.INFO_OWNER, VIEW_GID = FileManager.INFO_GID,
            VIEW_GROUP = FileManager.INFO_GROUP, VIEW_MODE = FileManager.INFO_MODE,
            VIEW_TYPE = FileManager.INFO_MIME, VIEW_INFO = FileManager.INFO_DESC;
    // right field of status bar (label 2, label 3)
    private final int STATUS_RIGHT_WIDTH = 160;
    // frame components
    private JMenuBar mainMenu = new JMenuBar();
    private JPanel topBar = new JPanel(new BorderLayout());
    private JToolBar toolBar = new JToolBar(), addressBar = new JToolBar();
    private JLabel addressLabel = new JLabel();
    private JTextField addressEdit = new JTextField();
    private JButton addressBtn = new JButton();
    private FileManager fileMgr;
    private OptionsFrame opt;
    private JPanel statusBar = new JPanel(new BorderLayout()),
            statusBarRight = new JPanel(new GridLayout(1, 2));
    private JLabel statusLabel1 = new JLabel(), statusLabel2 = new JLabel(),
            statusLabel3 = new JLabel();
    private HelpFrame helpFrm;
    // construct frame

    public RootFrame(Main mainClass) {
        // create actions to icons map (for toolbar)
        actionIcons.setProperty(ACTION_CUT, IconTheme.MAIN_DCUT);
        actionIcons.setProperty(ACTION_COPY, IconTheme.MAIN_COPY);
        actionIcons.setProperty(ACTION_PASTE, IconTheme.MAIN_PAST);
        actionIcons.setProperty(ACTION_COPYTO, IconTheme.MAIN_CPTO);
        actionIcons.setProperty(ACTION_MOVETO, IconTheme.MAIN_MVTO);
        actionIcons.setProperty(ACTION_DELETE, IconTheme.MAIN_DELE);
        actionIcons.setProperty(ACTION_PROPERTIES, IconTheme.MAIN_PROP);
        actionIcons.setProperty(ACTION_GOUP, IconTheme.MAIN_GOUP);
        actionIcons.setProperty(ACTION_GOHOME, IconTheme.MAIN_HOME);
        actionIcons.setProperty(ACTION_REFRESH, IconTheme.MAIN_REFR);
        actionIcons.setProperty(ACTION_MOUNT, IconTheme.MAIN_DMNT);
        actionIcons.setProperty(ACTION_UNMOUNT, IconTheme.MAIN_UMNT);
        actionIcons.setProperty(ACTION_EJECT, IconTheme.MAIN_EJCT);
        actionIcons.setProperty(ACTION_VIEWTILE, IconTheme.MAIN_TILE);
        actionIcons.setProperty(ACTION_VIEWICON, IconTheme.MAIN_ICON);
        actionIcons.setProperty(ACTION_VIEWLIST, IconTheme.MAIN_LIST);
        actionIcons.setProperty(ACTION_VIEWDETAILS, IconTheme.MAIN_DETA);
        // create actions to view styles (and invert)
        viewStyles.setProperty(ACTION_VIEWTILE, String.valueOf(FileViewer.VIEW_TILE));
        viewStyles.setProperty(ACTION_VIEWICON, String.valueOf(FileViewer.VIEW_ICON));
        viewStyles.setProperty(ACTION_VIEWLIST, String.valueOf(FileViewer.VIEW_LIST));
        viewStyles.setProperty(ACTION_VIEWDETAILS, String.valueOf(FileViewer.VIEW_DETAILS));
        viewStyles.setProperty(String.valueOf(FileViewer.VIEW_TILE), ACTION_VIEWTILE);
        viewStyles.setProperty(String.valueOf(FileViewer.VIEW_ICON), ACTION_VIEWICON);
        viewStyles.setProperty(String.valueOf(FileViewer.VIEW_LIST), ACTION_VIEWLIST);
        viewStyles.setProperty(String.valueOf(FileViewer.VIEW_DETAILS), ACTION_VIEWDETAILS);
        // create actions to sort types (and invert)
        sortTypes.setProperty(ACTION_SORTNAME, String.valueOf(FileViewer.SORT_NAME));
        sortTypes.setProperty(ACTION_SORTSIZE, String.valueOf(FileViewer.SORT_SIZE));
        sortTypes.setProperty(ACTION_SORTTYPE, String.valueOf(FileViewer.SORT_TYPE));
        sortTypes.setProperty(ACTION_SORTDATE, String.valueOf(FileViewer.SORT_DATE));
        sortTypes.setProperty(String.valueOf(FileViewer.SORT_NAME), ACTION_SORTNAME);
        sortTypes.setProperty(String.valueOf(FileViewer.SORT_SIZE), ACTION_SORTSIZE);
        sortTypes.setProperty(String.valueOf(FileViewer.SORT_TYPE), ACTION_SORTTYPE);
        sortTypes.setProperty(String.valueOf(FileViewer.SORT_DATE), ACTION_SORTDATE);
        // create gui
        try {
            // main class
            main = mainClass;
            // icon theme
            iconTheme = new IconTheme(main.conf.iconThemeDir,
                    main.conf.getProperty(Main.ConfigDb.GLOB_ROOT_ICONS));
            // setup frame
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    closeFrame();
                }
            });
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setFont(new Font("SansSerif", Font.PLAIN, 10));
            setIconImage(iconTheme.getIcon(File.listRoots()[0].getPath(), "inode/directory").getImage());
            setJMenuBar(mainMenu);
            setLayout(new BorderLayout());
            setLocationByPlatform(true);
            setSize(
                    Integer.parseInt(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM
                    + Main.ConfigDb.COMP_WIDTH)),
                    Integer.parseInt(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM
                    + Main.ConfigDb.COMP_HEIGHT)));
            setTitle(main.conf.getProperty(Main.ConfigDb.FM_ROOT_START));
            // setup menu
            mainMenu.setFont(getFont());
            buildMenu(mainMenu, Main.ConfigDb.FM_ROOT_FRM_MENU);
            // setup components
            topBar.setFont(getFont());
            toolBar.setFloatable(false);
            toolBar.setFont(getFont());
            toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
            for (int i = 0; i < Integer.parseInt(
                    main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_TOOL
                    + Main.ConfigDb.COMP_COUNT)); i++) {
                // if don't have action - add separator
                if (main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_TOOL + "." + i
                        + Main.ConfigDb.COMP_ACTION) == null) {
                    toolBar.addSeparator();
                } else // add button
                {
                    JButton btn = new JButton(new ImageIcon(iconTheme.getIcon(
                            actionIcons.getProperty(
                            main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_TOOL + "." + i
                            + Main.ConfigDb.COMP_ACTION), IconTheme.MAIN_DEFA), null)
                            .getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
                    btn.addActionListener(this);
                    btn.setFocusable(false);
                    btn.setFont(toolBar.getFont());
                    btn.setMargin(new Insets(1, 1, 1, 1));
                    btn.setName(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_TOOL + "." + i
                            + Main.ConfigDb.COMP_ACTION));
                    btn.setToolTipText(actionIcons.getProperty(
                            main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_TOOL + "." + i
                            + Main.ConfigDb.COMP_ACTION), IconTheme.MAIN_DEFA));
                    toolBar.add(btn);
                }
            }
            addressBar.setFloatable(false);
            addressBar.setFont(getFont());
            addressBar.setLayout(new BorderLayout());
            addressLabel.setFont(addressBar.getFont());
            addressLabel.setText(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_ADDR
                    + ".1" + Main.ConfigDb.COMP_TEXT));
            addressEdit.addActionListener(this);
            addressEdit.setFont(addressBar.getFont());
            addressEdit.setName(ACTION_GO);
            addressEdit.setText(main.conf.getProperty(Main.ConfigDb.FM_ROOT_START));
            addressBtn.addActionListener(this);
            addressBtn.setFocusable(false);
            addressBtn.setFont(addressBar.getFont());
            addressBtn.setName(ACTION_GO);
            addressBtn.setText(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_ADDR
                    + ".2" + Main.ConfigDb.COMP_TEXT));
            addressBar.add(addressLabel, BorderLayout.WEST);
            addressBar.add(addressEdit, BorderLayout.CENTER);
            addressBar.add(addressBtn, BorderLayout.EAST);
            topBar.add(toolBar, BorderLayout.NORTH);
            topBar.add(addressBar, BorderLayout.SOUTH);
            fileMgr = new FileManager(main, main.conf.getProperty(Main.ConfigDb.FM_ROOT_START),
                    iconTheme, statusLabel1, statusLabel2);
            // select file action
            fileMgr.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent ev) {
                    if (!fileMgr.isSelectionEmpty()
                            && (fileMgr.getModel().getSize() > 0)) // if selected & fmgr not empty
                    {
                        long size = 0;
                        Object[] items = fileMgr.getSelectedValues();
                        UnixFile file = (UnixFile) items[0];
                        if (items.length == 1) // 1 selected items
                        {
                            statusLabel1.setText(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_STAT
                                    + ".1.1" + Main.ConfigDb.COMP_TEXT)
                                    .replace(RootFrame.VIEW_NAME, file.getName())
                                    .replace(RootFrame.VIEW_SIZE, file.getSize())
                                    .replace(RootFrame.VIEW_DATE, String.valueOf(new Date(file.lastCreated())))
                                    .replace(RootFrame.VIEW_UID, String.valueOf(file.getUid()))
                                    .replace(RootFrame.VIEW_OWNER, file.getOwner())
                                    .replace(RootFrame.VIEW_GID, String.valueOf(file.getGid()))
                                    .replace(RootFrame.VIEW_GROUP, file.getGroup())
                                    .replace(RootFrame.VIEW_MODE, file.getMode())
                                    .replace(RootFrame.VIEW_TYPE, file.getMimeInfo())
                                    .replace(RootFrame.VIEW_INFO, file.getMimeDesc()));
                        } else // >1 selected items
                        {
                            statusLabel1.setText(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_STAT
                                    + ".1.2" + Main.ConfigDb.COMP_TEXT) + " " + items.length);
                        }
                        // sum size
                        for (int i = 0; i < items.length; i++) {
                            file = (UnixFile) items[i];
                            size = size + file.length();
                        }
                        statusLabel2.setText(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_STAT
                                + ".2" + Main.ConfigDb.COMP_TEXT) + " " + UnixFile.sizeToHuman(size));
                    }
                }
            });
            // open file action
            fileMgr.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent ev) {
                    if (ev.getButton() == MouseEvent.BUTTON1 && ev.getClickCount() == 2) {
                        fileMgr.openFile();
                        updateFrame();
                    }
                }
            });
            // load filemanager params
            fileMgr.setViewStyle(Integer.parseInt(viewStyles.getProperty(
                    main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE
                    + ".1" + Main.ConfigDb.COMP_ACTION)))); // view style
            fileMgr.setSortType(Integer.parseInt(sortTypes.getProperty(
                    main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE
                    + ".2" + Main.ConfigDb.COMP_ACTION)))); // sort type
            fileMgr.initialize();
            // set filemanager popup menu
            JMenu fmgrMenu = new JMenu();
            JPopupMenu fmgrPopup = new JPopupMenu();
            fmgrMenu.setFont(getFont());
            fmgrPopup.setFont(getFont());
            buildMenu(fmgrMenu, Main.ConfigDb.FM_ROOT_FRM_FILE_POPUP);
            while (fmgrMenu.getMenuComponentCount() > 0) {
                fmgrPopup.add(fmgrMenu.getMenuComponent(0));
            }
            fileMgr.setComponentPopupMenu(fmgrPopup);
            // set filemanager dnd actions
            fileMgr.setTransferHandler(new TransferHandler() {
                // export data
                protected Transferable createTransferable(JComponent c) {
                    FileManager fmgr = (FileManager) c;
                    Object[] list = fmgr.getSelectedValues();
                    StringBuffer buff = new StringBuffer();
                    for (int i = 0; i < list.length; i++) {
                        UnixFile file = (UnixFile) list[i];
                        try {
                            buff.append(file.toURI() + "\n"); // get file uri (example: "file:///root")
                            //buff.append(file.toString() + "\n"); // get file uri (example: "file:///root")
                        } catch (Exception ex) {
                        }
                        //System.out.println(file.getPath() + " => " + file.toURI());
                    }
                    buff.append("\n");
                    return new StringSelection(buff.toString());
                }

                public int getSourceActions(JComponent c) {
                    return COPY;
                }
                // import data

                public boolean importData(JComponent c, Transferable t) {
                    if (canImport(c, t.getTransferDataFlavors())) {
                        try {
                            FileManager fmgr = (FileManager) c;
                            String str = (String) t.getTransferData(DataFlavor.stringFlavor);
                            UnixFile[] list = new UnixFile[str.split("\n").length];
                            for (int i = 0; i < list.length; i++) // get file name from uri (remove end of line - "\n")
                            {
                                list[i] = new UnixFile(new URI(str.split("\n")[i].substring(0, str.split("\n")[i].length() - 1)));
                            }
                            fmgr.fileOperation(list, new UnixFile(fmgr.getCwd()), true, false);
                            return true;
                        } catch (Exception ex) {
                        }
                    }
                    return false;
                }
                // check import

                public boolean canImport(JComponent c, DataFlavor[] flavors) {
                    for (int i = 0; i < flavors.length; i++) {
                        if (DataFlavor.stringFlavor.equals(flavors[i])) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            // load other filemanager options
            fileMgr.setFont(getFont());
            fileMgr.setIconImage(getIconImage());
            fileMgr.setComment1Option(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_ITEMS
                    + ".1" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setComment2Option(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_ITEMS
                    + ".2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setDetailsOption(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_ITEMS
                    + ".3" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setToolTipOption(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_ITEMS
                    + ".4" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setCopyInfo(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".1.1" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setCopyError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".1.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setCopyQuestion(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".1.3" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setDeleteInfo(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".2.1" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setDeleteError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".2.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setDeleteQuestion(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".2.3" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setAccessError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".3.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setMkdirError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".4.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setMkdirQuestion(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".4.3" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setCreateError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".5.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setCreateQuestion(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".5.3" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setRenameError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".6.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setRenameQuestion(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".6.3" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setMountError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".7.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setUnmountError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".8.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setEjectError(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_FILE_FRAMES
                    + ".9.2" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setIdleOption(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_STAT
                    + ".1.3" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setLoadingOption(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_STAT
                    + ".1.4" + Main.ConfigDb.COMP_TEXT));
            fileMgr.setSizeOption(main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_STAT
                    + ".2" + Main.ConfigDb.COMP_TEXT));

            opt = new OptionsFrame(main, fileMgr);

            statusBar.setFont(getFont());
            statusLabel1.setFont(statusBar.getFont());
            statusBarRight.setFont(statusBar.getFont());
            statusBarRight.setPreferredSize(new Dimension(STATUS_RIGHT_WIDTH, statusBarRight.getHeight()));
            statusLabel2.setFont(statusBar.getFont());
            statusLabel3.setFont(statusBar.getFont());
            statusBarRight.add(statusLabel2);
            statusBarRight.add(statusLabel3);
            statusBar.add(statusLabel1, BorderLayout.CENTER);
            statusBar.add(statusBarRight, BorderLayout.EAST);
            helpFrm = new HelpFrame(this);
            getContentPane().add(topBar, BorderLayout.NORTH);
            getContentPane().add(new JScrollPane(fileMgr), BorderLayout.CENTER);
            getContentPane().add(statusBar, BorderLayout.SOUTH);
            // finalize
            fileMgr.setCwd(fileMgr.getCwd());
            updateFrame();
            setVisible(true);
        } catch (Exception ex) {
            fx.Main.killApp("frame", "error while initializing", ex);
        }
    }
    // create (sub)menu

    private void buildMenu(JComponent root, String config) {
        JMenu rootMenu;
        try {
            // create subitems
            for (int i = 0; i < Integer.parseInt(main.conf.getProperty(config + Main.ConfigDb.COMP_COUNT)); i++) {
                if (main.conf.getProperty(config + "." + i + Main.ConfigDb.COMP_COUNT) == null) // if don't have subitems
                {
                    if (main.conf.getProperty(config + "." + i + Main.ConfigDb.COMP_TEXT) == null) // if don't have name - add separator
                    {
                        rootMenu = (JMenu) root;
                        rootMenu.addSeparator();
                    } else // if have name
                    {
                        JMenuItem item = new JMenuItem(main.conf.getProperty(config + "." + i + Main.ConfigDb.COMP_TEXT));
                        item.addActionListener(this);
                        item.setAccelerator(KeyStroke.getKeyStroke(main.conf.getProperty(config + "." + i + Main.ConfigDb.COMP_SHORTCUT)));
                        item.setFont(root.getFont());
                        item.setName(main.conf.getProperty(config + "." + i + Main.ConfigDb.COMP_ACTION));
                        root.add(item);
                    }
                } else // if nave subitems
                {
                    JMenu menu = new JMenu(main.conf.getProperty(config + "." + i + Main.ConfigDb.COMP_TEXT));
                    menu.setFont(root.getFont());
                    root.add(menu);
                    buildMenu(menu, config + "." + i);
                }
            }
        } catch (Exception ex) {
            fx.Main.killApp("frame", "menu building error");
        }
    }
    // close frame (set new settings, save settings and exit)

    private void closeFrame() {
        // save frame size
        main.conf.setProperty(Main.ConfigDb.FM_ROOT_FRM
                + Main.ConfigDb.COMP_WIDTH, String.valueOf(getWidth())); // frame width
        main.conf.setProperty(Main.ConfigDb.FM_ROOT_FRM
                + Main.ConfigDb.COMP_HEIGHT, String.valueOf(getHeight())); // frame height
        // save filemanager params
        main.conf.setProperty(Main.ConfigDb.FM_ROOT_FRM_FILE
                + ".1" + Main.ConfigDb.COMP_ACTION, viewStyles.getProperty(
                String.valueOf(fileMgr.getViewStyle()),
                String.valueOf(FileManager.VIEW_TILE))); // view style
        main.conf.setProperty(Main.ConfigDb.FM_ROOT_FRM_FILE
                + ".2" + Main.ConfigDb.COMP_ACTION, sortTypes.getProperty(
                String.valueOf(fileMgr.getSortType()),
                String.valueOf(FileManager.SORT_NAME))); // sort type
        // exit app
        fx.Main.exitApp(main);
    }
    // update gui (update title, address field, status bar - free space)

    private void updateFrame() {
        setTitle(fileMgr.getCwd());
        addressEdit.setText(fileMgr.getCwd());
        statusLabel3.setText(
                main.conf.getProperty(Main.ConfigDb.FM_ROOT_FRM_STAT + ".3" + Main.ConfigDb.COMP_TEXT)
                + " " + UnixFile.sizeToHuman(100)); //NativeUtils.getFreeSpace(fileMgr.getCwd())));
    }

    // actions
    public void actionPerformed(ActionEvent e) {
        try {
            JComponent source = (JComponent) e.getSource(); // i know, this is JComponent or extends JComponent
            // action stored in the component's name
            if (source.getName().equals(ACTION_EXIT)) // exit
            {
                closeFrame();
            }
            if (source.getName().equals(ACTION_REFRESH)) // refresh
            {
                fileMgr.setCwd(fileMgr.getCwd());
            }
            if (source.getName().equals(ACTION_GO)) // cd
            {
                fileMgr.setCwd(addressEdit.getText());
            }
            if (source.getName().equals(ACTION_GOUP)) // up
            {
                fileMgr.goUp();
            }
            if (source.getName().equals(ACTION_GOHOME)) // home
            {
                fileMgr.setCwd(System.getProperty("user.home"));
            }
            if (source.getName().equals(ACTION_GOROOT)) // go to root directory
            {
                fileMgr.setCwd(File.listRoots()[0].getPath());
            }
            if (source.getName().equals(ACTION_VIEWTILE)) // tile view
            {
                fileMgr.setViewStyle(FileManager.VIEW_TILE);
            }
            if (source.getName().equals(ACTION_VIEWICON)) // icon view
            {
                fileMgr.setViewStyle(FileManager.VIEW_ICON);
            }
            if (source.getName().equals(ACTION_VIEWLIST)) // list view
            {
                fileMgr.setViewStyle(FileManager.VIEW_LIST);
            }
            if (source.getName().equals(ACTION_VIEWDETAILS)) // details view
            {
                fileMgr.setViewStyle(FileManager.VIEW_DETAILS);
            }
            if (source.getName().equals(ACTION_SORTNAME)) // sort by name
            {
                fileMgr.setSortType(FileManager.SORT_NAME);
            }
            if (source.getName().equals(ACTION_SORTSIZE)) // sort by size
            {
                fileMgr.setSortType(FileManager.SORT_SIZE);
            }
            if (source.getName().equals(ACTION_SORTTYPE)) // sort by type
            {
                fileMgr.setSortType(FileManager.SORT_TYPE);
            }
            if (source.getName().equals(ACTION_SORTDATE)) // sort by date
            {
                fileMgr.setSortType(FileManager.SORT_DATE);
            }
            if (source.getName().equals(ACTION_SELALL)) // select all
            {
                fileMgr.selectAll();
            }
            if (source.getName().equals(ACTION_INVSEL)) // invert selection
            {
                fileMgr.invertSelection();
            }
            if (source.getName().equals(ACTION_OPEN)) // open
            {
                fileMgr.openFile();
            }
            if (source.getName().equals(ACTION_OPENWITH)) // open with
            {
                fileMgr.openFileWith();
            }
            if (source.getName().equals(ACTION_PROPERTIES)) // properties
            {
                fileMgr.showProperties();
            }
            if (source.getName().equals(ACTION_RENAME)) // rename
            {
                fileMgr.renameFile();
            }
            if (source.getName().equals(ACTION_DELETE)) // delete
            {
                fileMgr.deleteFile();
            }
            if (source.getName().equals(ACTION_CUT)) // cut
            {
                fileMgr.cut();
            }
            if (source.getName().equals(ACTION_COPY)) // copy
            {
                fileMgr.copy();
            }
            if (source.getName().equals(ACTION_PASTE)) // paste
            {
                fileMgr.paste();
            }
            if (source.getName().equals(ACTION_PASTELINK)) // paste symlink
            {
                fileMgr.pasteLnk();
            }
            if (source.getName().equals(ACTION_MKDIR)) // mkdir
            {
                fileMgr.makeDir();
            }
            if (source.getName().startsWith(ACTION_BOOK)) // go to bookmark
            {
                String path = source.getName().substring(ACTION_BOOK.length(),
                        source.getName().length());
                fileMgr.setCwd(path);
            }
            if (source.getName().equals(ACTION_MOUNT)) // mount
            {
                fileMgr.mountDir();
            }
            if (source.getName().equals(ACTION_UNMOUNT)) // unmount
            {
                fileMgr.unmountDir();
            }
            if (source.getName().equals(ACTION_EJECT)) // unmount
            {
                fileMgr.ejectDir();
            }
            if (source.getName().equals(ACTION_OPTIONS)) // options
            {
                opt.showOptions();
            }
            if (source.getName().equals(ACTION_HELP)) // show help
            {
                helpFrm.showHelp();
            }
            if (source.getName().equals(ACTION_ABOUT)) // show about box
            {
                JOptionPane.showMessageDialog(null,
                        "XionDE.fm - XionDE File Manager\n"
                        + "Version 0.2.1.3\n"
                        + "Copyright (C) 2007 XionDE TEAM\n"
                        + "http://xionde.vectorstar.net", "About", JOptionPane.PLAIN_MESSAGE);
            }


            updateFrame(); // update gui
        } catch (Exception ex) {
            fx.Main.killApp("frame", "error while executing internal action command");
        }
    }
}
