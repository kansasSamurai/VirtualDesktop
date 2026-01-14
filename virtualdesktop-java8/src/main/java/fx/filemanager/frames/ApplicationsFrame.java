/*
   XionDE.fm - XionDE File Manager
   Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

   This program can be distributed under the terms of the GNU GPL
   See the file COPYING.
*/

// file manager frames package
package fx.filemanager.frames;
// import what i need
import java.awt.*; // for compatibility
import java.awt.event.*; // for events
import java.util.Vector; // apps list
import javax.swing.*; // for swing gui
import javax.swing.event.*; // for extended events
import fx.filemanager.FileManager; // owner
import fx.filemanager.NativeUtils; // exec apps
import fx.filemanager.UnixFile; // open files
// applications list frame class
public class ApplicationsFrame extends JFrame
{
 // components params
 private final Dimension ROOT_SIZE = new Dimension(400, 400); // root size
 // components
 private JPanel mainPanel = new JPanel(new BorderLayout()),
   lowPanel = new JPanel(new BorderLayout()),
   btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)),
   cntPanel = new JPanel(new BorderLayout()),
   topcntPanel = new JPanel(new GridLayout(1, 5)),
   subcntPanel = new JPanel(new GridLayout(4, 1));
 private JButton okBtn = new JButton(), cancelBtn = new JButton(),
   btn1 = new JButton(), btn2 = new JButton(), btn3 = new JButton(),
   btn4 = new JButton();
 private JList appsList = new JList();
 private JTextField cmdEdit = new JTextField();
 private JCheckBox
   app1Box = new JCheckBox(), // ext (*.txt)
   app2Box = new JCheckBox(), // local mime (text/plain)
   app3Box = new JCheckBox(); // global mime (text/*)
 private fx.Main main;
 private FileManager fmgr;
 private UnixFile item;
 private Vector apps = new Vector(), appsExt = new Vector(),
   appsLocMime = new Vector(), appsGlobMime = new Vector(),
   appsAll = new Vector();
 private String ext = null, mime = null;
 private boolean appsListReady = true;
 // construct frame
 public ApplicationsFrame(fx.Main owner, FileManager manager, UnixFile file)
 {
  try
  {
   // setup frame
   main = owner; // main module - owner
   fmgr = manager;
   item = file;
   addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { closeApps(); } });
   setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
   setFont(fmgr.getFont());
   setIconImage(fmgr.getIconImage());
   setLayout(new BorderLayout());
   setResizable(false);
   setSize(ROOT_SIZE);
   setLocation((Toolkit.getDefaultToolkit().getScreenSize().width / 2) - (getWidth() / 2),
     (Toolkit.getDefaultToolkit().getScreenSize().height / 2) - (getHeight() / 2));
   setTitle("Select application");
   // setup components
   okBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { startApps(); } });
   okBtn.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   okBtn.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   okBtn.setFont(getFont());
   okBtn.setText(fmgr.getFilePropsButtons().split("#")[1]);
   cancelBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { closeApps(); } });
   cancelBtn.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   cancelBtn.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   cancelBtn.setFont(getFont());
   cancelBtn.setText(fmgr.getFilePropsButtons().split("#")[2]);
   btnPanel.setFont(getFont());
   btnPanel.add(okBtn);
   btnPanel.add(cancelBtn);
   lowPanel.setFont(getFont());
   lowPanel.add(btnPanel, BorderLayout.EAST);
   btn1.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   btn1.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   btn1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { moveUp(); } });
   btn1.setFont(getFont());
   btn1.setText("Move Up");
   btn2.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   btn2.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   btn2.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { moveDown(); } });
   btn2.setFont(getFont());
   btn2.setText("Move Down");
   btn3.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   btn3.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   btn3.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { moveOut(); } });
   btn3.setFont(getFont());
   btn3.setText("Remove");
   btn4.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   btn4.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   btn4.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { byDefault(); } });
   btn4.setFont(getFont());
   btn4.setText("By default");
   topcntPanel.add(btn1);
   topcntPanel.add(btn2);
   topcntPanel.add(btn3);
   topcntPanel.add(btn4);
   appsList.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   appsList.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   appsList.addListSelectionListener(new ListSelectionListener() { public void valueChanged(ListSelectionEvent ev)
   {
    if (appsListReady)
    {
     String sel = (String) apps.elementAt(appsList.getSelectedIndex());
     sel = sel.substring(sel.indexOf("]") + 2, sel.length());
     if (sel.endsWith("<-default"))
     sel = sel.substring(0, sel.length() - "<-default".length() - 1);
     cmdEdit.setText(sel);
    }
   } } );
   appsList.setFont(getFont());
   appsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
   cntPanel.setFont(getFont());
   //cntPanel.setSize(getSize().width, getSize().height);
   cntPanel.add(new JScrollPane(appsList), BorderLayout.CENTER);
   cmdEdit.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   cmdEdit.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   cmdEdit.setFont(getFont());
   cmdEdit.setText("myapp %s");
   app1Box.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   app1Box.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   app1Box.setFont(getFont());
   try
   {
    ext = item.getName().substring(item.getName().lastIndexOf("."), item.getName().length());
   } catch (Exception ex) {}
   if (ext == null) ext = "";
   app1Box.setText("always open with this application (by name: *" + ext + ")");
   app2Box.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   app2Box.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   app2Box.setFont(getFont());
   app2Box.setText("always open with this application (by mime: " + item.getMimeInfo() + ")");
   app3Box.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) closeApps(); } });
   app3Box.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ENTER) startApps(); } });
   app3Box.setFont(getFont());
   try
   {
    mime = item.getMimeInfo().split("/")[0] + "/*";
   } catch (Exception ex) {}
   if (mime == null) mime = item.getMimeInfo();
   app3Box.setText("always open with this application (by mime: " + mime + ")");
   subcntPanel.setFont(getFont());
   subcntPanel.add(cmdEdit);
   subcntPanel.add(app1Box);
   subcntPanel.add(app2Box);
   subcntPanel.add(app3Box);
   mainPanel.setFont(getFont());
   mainPanel.add(cntPanel, BorderLayout.CENTER);
   mainPanel.add(topcntPanel, BorderLayout.NORTH);
   mainPanel.add(subcntPanel, BorderLayout.SOUTH);
   getContentPane().add(mainPanel, BorderLayout.CENTER);
   getContentPane().add(lowPanel, BorderLayout.SOUTH);
   loadAppsList();
  } catch (Exception ex) { fx.Main.killApp("appslist", "error while initializing frame"); }
 }
 // load application list
 private void loadAppsList()
 {
  try
  {
   appsListReady = false;
   appsList.clearSelection();
   apps.removeAllElements();
   int i;
   i = 0;
   while (main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS
            + ".*" + ext + "." + i) != null)
   {
    appsAll.addElement(fx.Main.ConfigDb.FM_ROOT_APPS + ".*" + ext + "." + i);
    appsExt.addElement(fx.Main.ConfigDb.FM_ROOT_APPS + ".*" + ext + "." + i);
    apps.addElement("[by name: *" + ext + "] "
      + main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS
      + ".*" + ext + "." + i));    
    i++;
   }
   i = 0;
   while (main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS
            + "." + item.getMimeInfo() + "." + i) != null)
   {
    appsAll.addElement(fx.Main.ConfigDb.FM_ROOT_APPS
      + "." + item.getMimeInfo() + "." + i);
    appsLocMime.addElement(fx.Main.ConfigDb.FM_ROOT_APPS
      + "." + item.getMimeInfo() + "." + i);
    apps.addElement("[by mime: " + item.getMimeInfo() + "] "
      + main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS
      + "." + item.getMimeInfo() + "." + i));    
    i++;
   }
   i = 0;
   while (main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS
            + "." + mime + "." + i) != null)
   {
    appsAll.addElement(fx.Main.ConfigDb.FM_ROOT_APPS + "." + mime + "." + i);
    appsGlobMime.addElement(fx.Main.ConfigDb.FM_ROOT_APPS + "." + mime + "." + i);
    apps.addElement("[by mime: " + mime + "] "
      + main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS
      + "." + mime + "." + i));    
    i++;
   }
   if (apps.size() > 0)
   apps.setElementAt(apps.elementAt(0) + " <-default", 0);
   appsList.setListData(apps);
   appsListReady = true;
  } catch (Exception ex) {}
 }
 // get application group (ext (1), local mime (2), global mime (3)) by index
 public int getAppsGroup(int index)
 {
  if (index >= 0 &&
      index < appsExt.size()) return 1;
  if (index >= appsExt.size() &&
      index < appsLocMime.size() + appsExt.size()) return 2;
  if (index >= appsLocMime.size() + appsExt.size() &&
      index < appsAll.size()) return 3;
  return -1;
 }
 // get application group index
 public int getAppsGroupIndex(int group, int index)
 {
  if (group == 1) return index;
  if (group == 2) return index - appsExt.size();
  if (group == 3) return index - appsExt.size() - appsLocMime.size();
  return -1;
 }
 // get application group index - frontend
 public int getAppsGroupIndex(int index)
 {
  return getAppsGroupIndex(getAppsGroup(index), index);
 }
 // swap applications
 public void swapApps(int index1, int index2)
 {
  String s1, s2;
  s1 = main.conf.getProperty((String) appsAll.elementAt(index1));
  s2 = main.conf.getProperty((String) appsAll.elementAt(index2));
  main.conf.setProperty((String) appsAll.elementAt(index1), s2);
  main.conf.setProperty((String) appsAll.elementAt(index2), s1);
 }
 // display applications list
 public void showApps()
 {
  setVisible(true);
 }
 // start app
 public void startApps()
 {
  JToggleButton.ToggleButtonModel chk1 = (JToggleButton.ToggleButtonModel) app1Box.getModel();
  JToggleButton.ToggleButtonModel chk2 = (JToggleButton.ToggleButtonModel) app2Box.getModel();
  JToggleButton.ToggleButtonModel chk3 = (JToggleButton.ToggleButtonModel) app3Box.getModel();
  if (chk1.isSelected())
  {
   main.conf.setProperty(fx.Main.ConfigDb.FM_ROOT_APPS
      + ".*" + ext + "." + 0, cmdEdit.getText());
  }
  if (chk2.isSelected())
  {
   main.conf.setProperty(fx.Main.ConfigDb.FM_ROOT_APPS
      + "." + item.getMimeInfo() + "." + 0, cmdEdit.getText());
  }
  if (chk3.isSelected())
  {
   main.conf.setProperty(fx.Main.ConfigDb.FM_ROOT_APPS
      + "." + mime + "." + 0, cmdEdit.getText());
  }
  String app = cmdEdit.getText().replace("%s", item.getPath().replace(" ", "%20"));
  NativeUtils.execApp(app);
  closeApps();
 }
 // close applications list
 public void closeApps()
 {
  setVisible(false);
 }
 // top buttons actions
 public void moveUp()
 {
  if (!appsList.isSelectionEmpty())
  if (getAppsGroupIndex(appsList.getSelectedIndex()) > 0)
  {
   swapApps(appsList.getSelectedIndex(), appsList.getSelectedIndex() - 1);
   loadAppsList();
  }
 }
 public void moveDown()
 {
  if (!appsList.isSelectionEmpty())
//  if ((getAppsGroup(appsList.getSelectedIndex()) == 1 &&
//       getAppsGroupIndex(appsList.getSelectedIndex()) < appsExt.size() - 1) ||
//      (getAppsGroup(appsList.getSelectedIndex()) == 2 &&
//       getAppsGroupIndex(appsList.getSelectedIndex()) < appsLocMime.size() - 1) ||
//      (getAppsGroup(appsList.getSelectedIndex()) == 3 &&
//       getAppsGroupIndex(appsList.getSelectedIndex()) < appsGlobMime.size() - 1))
  if (getAppsGroupIndex(appsList.getSelectedIndex()) < appsAll.size() - 1)
  {
   swapApps(appsList.getSelectedIndex(), appsList.getSelectedIndex() + 1);
   loadAppsList();
  }
 }
 public void moveOut()
 {
  if (!appsList.isSelectionEmpty())
  {
   main.conf.removeProperty((String) appsAll.elementAt(appsList.getSelectedIndex()));
   loadAppsList();
  }
 }
 public void byDefault()
 {
  if (!appsList.isSelectionEmpty())
  if (getAppsGroupIndex(appsList.getSelectedIndex()) > 0)
  {
   swapApps(appsList.getSelectedIndex(), 0);
   loadAppsList();
  } 
 }
}
