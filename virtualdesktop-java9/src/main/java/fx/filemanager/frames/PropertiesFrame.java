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
import java.util.Date; // show files dates (create, modify, access)
import javax.swing.*; // for swing gui
import fx.filemanager.FileManager; // owner
import fx.filemanager.NativeUtils; // for chmod/chown
import fx.filemanager.UnixFile; // to change unix file properties
// properties frame class
public class PropertiesFrame extends JFrame
{
 // components params
 private final Dimension ROOT_SIZE = new Dimension(350, 330); // root size
 private UnixFile[] files = null; // targets
 private boolean changed = false; // properties changed
 private String mode; // file mode
 // components
 private JPanel
   mainPanel  = new JPanel(new BorderLayout()),
   lowPanel   = new JPanel(new BorderLayout()),
   btnPanel   = new JPanel(new FlowLayout(FlowLayout.RIGHT)),
   leftPanel  = new JPanel(new GridLayout(12, 1)),
   rightPanel = new JPanel(new GridLayout(12, 1)),
   ownerPanel = new JPanel(new GridLayout(1, 4)),
   groupPanel = new JPanel(new GridLayout(1, 4)),
   otherPanel = new JPanel(new GridLayout(1, 4));
 private JButton okBtn = new JButton(), cancelBtn = new JButton(), applyBtn = new JButton();
 private JCheckBox[][] modeBox = new JCheckBox[3][4]; // [x][y]: x=u/g/o; y=r/w/x/s;
 private JCheckBox recurBox = new JCheckBox();
 private JLabel[] titleLabels = new JLabel[12];
 private JLabel mimeLabel = new JLabel(), locLabel = new JLabel(), sizeLabel = new JLabel(),
   createLabel = new JLabel(), modifLabel = new JLabel(), accessLabel = new JLabel(),
   modeLabel = new JLabel(), ownerLabel = new JLabel(), groupLabel = new JLabel();
 private FileManager fmgr;
 private PropertiesThread threadSize = null, threadChmod = null;
 private String cwd;
 // construct frame
 public PropertiesFrame(FileManager owner)
 {
  try
  {
   // setup frame
   fmgr = owner; // set file manager - owner
   cwd = fmgr.getCwd(); // store cwd
   addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { hideFileProps(false); } });
   setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
   setFont(fmgr.getFont());
   setIconImage(fmgr.getIconImage());
   setLayout(new BorderLayout());
   setResizable(false);
   setSize(ROOT_SIZE);
   setLocation((Toolkit.getDefaultToolkit().getScreenSize().width / 2) - (getWidth() / 2),
     (Toolkit.getDefaultToolkit().getScreenSize().height / 2) - (getHeight() / 2));
   // setup components
   recurBox.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { changeMode(); } });
   recurBox.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) hideFileProps(false); } });
   recurBox.setFont(getFont());
   recurBox.setText(fmgr.getFilePropsButtons().split("#")[0]);
   okBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { hideFileProps(true); } });
   okBtn.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) hideFileProps(false); } });
   okBtn.setFont(getFont());
   okBtn.setText(fmgr.getFilePropsButtons().split("#")[1]);
   cancelBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { hideFileProps(false); } });
   cancelBtn.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) hideFileProps(false); } });
   cancelBtn.setFont(getFont());
   cancelBtn.setText(fmgr.getFilePropsButtons().split("#")[2]);
   applyBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { applyFileProps(); } });
   applyBtn.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) hideFileProps(false); } });
   applyBtn.setFont(getFont());
   applyBtn.setText(fmgr.getFilePropsButtons().split("#")[3]);
   btnPanel.setFont(getFont());
   btnPanel.add(okBtn);
   btnPanel.add(cancelBtn);
   btnPanel.add(applyBtn);
   lowPanel.add(recurBox, BorderLayout.CENTER);
   lowPanel.add(btnPanel, BorderLayout.EAST);
   leftPanel.setFont(getFont());
   leftPanel.setSize((getSize().width * 35) / 100, getSize().height);
   for (int i = 0; i < titleLabels.length; i++)
   {
    titleLabels[i] = new JLabel(fmgr.getFilePropsTitles().split("#")[i]);
    titleLabels[i].setFont(getFont());
    leftPanel.add(titleLabels[i]);
   }
   for (int x = 0; x < 3; x++)
   for (int y = 0; y < 4; y++)
   {
    modeBox[x][y] = new JCheckBox();
    modeBox[x][y].addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) { changeMode(); } });
    modeBox[x][y].addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { if (e.getKeyCode() == e.VK_ESCAPE) hideFileProps(false); } });
    modeBox[x][y].setFont(getFont());
   }
   for (int y = 0; y < 3; y++)
   for (int x = 0; x < 3; x++)
   modeBox[x][y].setText(fmgr.getFilePropsTitles().split("#")[12 + y]);
   modeBox[0][3].setText(fmgr.getFilePropsTitles().split("#")[15]);
   modeBox[1][3].setText(fmgr.getFilePropsTitles().split("#")[16]);
   modeBox[2][3].setText(fmgr.getFilePropsTitles().split("#")[17]);
   JPanel[] modePanel = { ownerPanel, groupPanel, otherPanel };
   for (int x = 0; x < 3; x++)
   for (int y = 0; y < 4; y++)
   {
    modePanel[x].setFont(getFont());       
    modePanel[x].add(modeBox[x][y]);
   }
   mimeLabel.setFont(getFont());
   locLabel.setFont(getFont());
   sizeLabel.setFont(getFont());
   createLabel.setFont(getFont());
   modifLabel.setFont(getFont());
   accessLabel.setFont(getFont());
   modeLabel.setFont(getFont());
   ownerLabel.setFont(getFont());
   groupLabel.setFont(getFont());
   rightPanel.setFont(getFont());
   rightPanel.add(mimeLabel);
   rightPanel.add(locLabel);
   rightPanel.add(sizeLabel);
   rightPanel.add(createLabel);
   rightPanel.add(modifLabel);
   rightPanel.add(accessLabel);
   rightPanel.add(modeLabel);
   rightPanel.add(ownerPanel);
   rightPanel.add(groupPanel);
   rightPanel.add(otherPanel);
   rightPanel.add(ownerLabel);
   rightPanel.add(groupLabel);
   mainPanel.setFont(getFont());
   mainPanel.add(leftPanel, BorderLayout.WEST);
   mainPanel.add(rightPanel, BorderLayout.CENTER);
   getContentPane().add(mainPanel, BorderLayout.NORTH);
   getContentPane().add(lowPanel, BorderLayout.SOUTH);
  } catch (Exception ex) { fx.Main.killApp("properties", "error while initializing frame"); }
 }
 // display file properties
 public void showFileProps(UnixFile[] targets)
 {
  changed = false;
  files = targets; // setup files list
  JCheckBox[] bitBox = { modeBox[0][3], modeBox[1][3], modeBox[2][3], modeBox[0][0],
                         modeBox[0][1], modeBox[0][2], modeBox[1][0], modeBox[1][1],
                         modeBox[1][2], modeBox[2][0], modeBox[2][1], modeBox[2][2] };
  JToggleButton.ToggleButtonModel chkModel = new JToggleButton.ToggleButtonModel();
  applyBtn.setEnabled(false);
  chkModel.setSelected(false);
  recurBox.setModel(chkModel);
  if (files.length == 1) // single file
  {
   setTitle(files[0].getName());
   mimeLabel.setText(files[0].getMimeInfo());
   Date ctime = new Date(files[0].lastCreated());
   Date mtime = new Date(files[0].lastModified());
   Date atime = new Date(files[0].lastAccessed());
   createLabel.setText(ctime.toString());
   modifLabel.setText(mtime.toString());
   accessLabel.setText(atime.toString());
   mode = files[0].getMode();
   modeLabel.setText(mode + " (" + Integer.toOctalString(UnixFile.modeToInt(mode)) + ")");
   for (int x = bitBox.length - 1, mask = 1; x >= 0; x--, mask = mask * 2)
   {
    chkModel = new JToggleButton.ToggleButtonModel();
    if ((UnixFile.modeToInt(mode) & mask) != 0) chkModel.setSelected(true);
    else chkModel.setSelected(false);
    bitBox[x].setModel(chkModel);
   }
   ownerLabel.setText(files[0].getOwner() + " (" + files[0].getUid() + ")");
   groupLabel.setText(files[0].getGroup() + " (" + files[0].getGid() + ")");
  }
  else // multiple files
  {
   setTitle("");
   mimeLabel.setText("...");
   createLabel.setText("...");
   modifLabel.setText("...");
   accessLabel.setText("...");
   modeLabel.setText("...");
   for (int x = 0; x < bitBox.length; x++) // unckeck all
   {
    chkModel = new JToggleButton.ToggleButtonModel();
    chkModel.setSelected(false);
    bitBox[x].setModel(chkModel);
   }
   for (int i = 0; i < files.length; i++) // set all need
   for (int x = bitBox.length - 1, mask = 1; x >= 0; x--, mask = mask * 2)
   {
    chkModel = new JToggleButton.ToggleButtonModel();
    chkModel.setSelected(true);
    if ((UnixFile.modeToInt(files[i].getMode()) & mask) != 0)
    bitBox[x].setModel(chkModel);
   }
   int modeInt = 0;
   for (int x = bitBox.length - 1, mask = 1; x >= 0; x--, mask = mask * 2) // set mode
   {
    chkModel = (JToggleButton.ToggleButtonModel) bitBox[x].getModel();
    if (chkModel.isSelected()) modeInt = modeInt | mask;
   }
   mode = UnixFile.modeToString(modeInt);
   modeLabel.setText(mode + " (" + Integer.toOctalString(UnixFile.modeToInt(mode)) + ")");
   ownerLabel.setText("...");
   groupLabel.setText("...");
  }
  locLabel.setText(files[0].getParent());
  threadSize = new PropertiesThread(sizeLabel, files, PropertiesThread.MODE_SIZE);
  threadSize.start(); // get size
  this.setVisible(true);
 }
 // close properties
 public void hideFileProps(boolean needApply)
 {
  if (needApply && changed) applyFileProps();
  threadSize.terminate();
  threadSize = null;
  this.setVisible(false);
 }
 // change file mode by set checkboxes
 private void changeMode()
 {
  if (!changed)
  {
   changed = true;
   applyBtn.setEnabled(true);
  }
  byte[] modeLine = mode.getBytes();
  for (int y = 0; y < 2; y++)
  for (int x = 0, f = y + 1; x < 3; x++, f = f + 3)
  {
   JToggleButton.ToggleButtonModel chkModel = (JToggleButton.ToggleButtonModel) modeBox[x][y].getModel();
   if (chkModel.isSelected())
   switch (y)
   {
    default: modeLine[f] = 'r'; break; // 0
    case 1: modeLine[f] = 'w'; break;
   }
   else modeLine[f] = '-';
  }
  for (int x = 0; x < 3; x++)
  {
   JToggleButton.ToggleButtonModel chk1Model = (JToggleButton.ToggleButtonModel) modeBox[x][2].getModel();
   JToggleButton.ToggleButtonModel chk2Model = (JToggleButton.ToggleButtonModel) modeBox[x][3].getModel();
   if (!chk1Model.isSelected() && !chk2Model.isSelected())
   modeLine[(x + 1) * 3] = '-';
   if (chk1Model.isSelected() && !chk2Model.isSelected())
   modeLine[(x + 1) * 3] = 'x';
   if (!chk1Model.isSelected() && chk2Model.isSelected())
   if (x < 2) modeLine[(x + 1) * 3] = 'S';
   else modeLine[(x + 1) * 3] = 'T';
   if (chk1Model.isSelected() && chk2Model.isSelected())
   if (x < 2) modeLine[(x + 1) * 3] = 's';
   else modeLine[(x + 1) * 3] = 't';
  }
  mode = new String(modeLine);
  modeLabel.setText(mode + " (" + Integer.toOctalString(UnixFile.modeToInt(mode)) + ")");
 }
 // apply changed (new) properties
 private void applyFileProps()
 {
  for (int i = 0; i < files.length; i++)
  chmod(files[i]);
  if (fmgr.getCwd().equals(cwd)) fmgr.setCwd(cwd); // if cwd is not changed - refresh
  changed = false;
  applyBtn.setEnabled(false);
 }
 // recursive chmod
 private void chmod(UnixFile file)
 {
  JToggleButton.ToggleButtonModel chkModel = (JToggleButton.ToggleButtonModel) recurBox.getModel();
  NativeUtils.setFileMode(file.getPath(), UnixFile.modeToInt(mode) & 07777);
  if (file.isDirectory() && chkModel.isSelected()) // if is directory and recursive mode set
  {
   try
   {
    UnixFile[] list = file.listFiles();
    if (list != null)
    for (int i = 0; i < list.length; i++)
    chmod(list[i]);
   } catch (Exception ex) { /* show error mesg */; }
  }
 }
}
