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
import java.beans.*; // property change listener
import java.io.*; // list themes
import java.util.Date; // show files dates (create, modify, access)
import javax.swing.*; // for swing gui
import fx.filemanager.FileManager; // owner
// options frame class
public class OptionsFrame extends JFrame
{
 // components params
 private final Dimension ROOT_SIZE = new Dimension(400, 400); // root size
 // components
 private JPanel mainPanel = new JPanel(new BorderLayout()),
   lowPanel = new JPanel(new BorderLayout()),
   btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)),
   leftPanel = new JPanel(new GridLayout(12, 1)),
   rightPanel = new JPanel(new GridLayout(12, 1));
 private JButton closeBtn = new JButton();
 private JComboBox themeBox = new JComboBox();
 private JTextField startdirEdit = new JTextField();
 private JLabel themeLabel = new JLabel("Icon Theme (requires restart):");
 private JLabel startdirLabel = new JLabel("Starting directory:");
 private fx.Main main;
 private FileManager fmgr;
 // construct frame
 public OptionsFrame(fx.Main owner, FileManager manager)
 {
  try
  {
   // setup frame
   main = owner; // main module - owner
   fmgr = manager;
   addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { hideOptions(); } });
   setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
   setFont(fmgr.getFont());
   setIconImage(fmgr.getIconImage());
   setLayout(new BorderLayout());
   setResizable(false);
   setSize(ROOT_SIZE);
   setLocation((Toolkit.getDefaultToolkit().getScreenSize().width / 2) - (getWidth() / 2),
     (Toolkit.getDefaultToolkit().getScreenSize().height / 2) - (getHeight() / 2));
   setTitle("Options");
   // setup components
   closeBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { hideOptions(); } });
   closeBtn.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) hideOptions(); } });
   closeBtn.setFont(getFont());
   closeBtn.setText(fmgr.getFilePropsButtons().split("#")[1]);
   btnPanel.setFont(getFont());
   btnPanel.add(closeBtn);
   lowPanel.add(btnPanel, BorderLayout.EAST);
   themeLabel.setFont(getFont());
   startdirLabel.setFont(getFont());
   leftPanel.setFont(getFont());
   leftPanel.setSize(getSize().width / 2, getSize().height);
   leftPanel.add(themeLabel);
   leftPanel.add(startdirLabel);
   themeBox.setFont(getFont());
   startdirEdit.setFont(getFont());
   rightPanel.setFont(getFont());
   rightPanel.add(themeBox);
   rightPanel.add(startdirEdit);
   mainPanel.setFont(getFont());
   mainPanel.add(leftPanel, BorderLayout.WEST);
   mainPanel.add(rightPanel, BorderLayout.CENTER);
   getContentPane().add(mainPanel, BorderLayout.NORTH);
   getContentPane().add(lowPanel, BorderLayout.SOUTH);
  } catch (Exception ex) { fx.Main.killApp("options", "error while initializing frame"); }
 }
 // display file properties
 public void showOptions()
 {
  try
  {
   File themeDir = new File(main.conf.iconThemeDir);
   String[] themeList = themeDir.list();
   themeBox.removeAllItems();
   // list themes, inverted order - new themes first
   int current = 0;
   for (int i = themeList.length - 1; i >= 0; i--, current++)
   {
    themeBox.addItem(themeList[i].substring(0, themeList[i].indexOf(".jar")));
    if (themeList[i].substring(0, themeList[i].indexOf(".jar"))
          .equals(main.conf.getProperty(fx.Main.ConfigDb.GLOB_ROOT_ICONS)))
    themeBox.setSelectedIndex(current);
   }
  } catch (Exception ex) { themeBox.removeAllItems(); }
  startdirEdit.setText(main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_START));
  this.setVisible(true);
 }
 // close properties
 public void hideOptions()
 {
  main.conf.setProperty(fx.Main.ConfigDb.GLOB_ROOT_ICONS,
    (String) themeBox.getSelectedItem());
  main.conf.setProperty(fx.Main.ConfigDb.FM_ROOT_START, startdirEdit.getText());
  this.setVisible(false);
 }
}
