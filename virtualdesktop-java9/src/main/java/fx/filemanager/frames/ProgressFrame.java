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
import javax.swing.*; // for swing gui
import fx.filemanager.FileManager; // to communicate
import fx.filemanager.UnixFile; // file operations
// progress frame class
public class ProgressFrame extends JFrame
{
 // components params
 private final Dimension ROOT_SIZE = new Dimension(400, 150); // root size
 private final int COMP_HGAP = 10, COMP_VGAP = 10;
 // components
 private JLabel statLabel = new JLabel(" ");
 private JProgressBar
   curProg = new JProgressBar(0, 1000), fullProg = new JProgressBar(0, 1000);
 private JPanel highPanel = new JPanel(new GridLayout(3, 1, COMP_HGAP, COMP_VGAP)),
   lowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
 private JButton cancelBtn = new JButton();
 private FileManager fmgr;
 private RewriteQuestionDialog rqDialog;
 private ProgressThread thread = null;
 private String cwd;
 // construct frame
 public ProgressFrame(FileManager owner)
 {
  try
  {
   // setup frame
   fmgr = owner; // set file manager - owner
   addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { stopProcess(); } });
   setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
   setFont(fmgr.getFont());
   setIconImage(fmgr.getIconImage());
   setLayout(new BorderLayout());
   setResizable(false);
   setSize(ROOT_SIZE);
   setLocation((Toolkit.getDefaultToolkit().getScreenSize().width / 2) - (getWidth() / 2),
     (Toolkit.getDefaultToolkit().getScreenSize().height / 2) - (getHeight() / 2));
   // setup components
   JPanel subPanel = new JPanel(new BorderLayout());
   JPanel nPanel = new JPanel(), sPanel = new JPanel(),
     wPanel = new JPanel(), ePanel = new JPanel();
   nPanel.setSize(COMP_HGAP, COMP_VGAP);
   sPanel.setSize(COMP_HGAP, COMP_VGAP);
   wPanel.setSize(COMP_HGAP, COMP_VGAP);
   ePanel.setSize(COMP_HGAP, COMP_VGAP);
   highPanel.setFont(getFont());
   statLabel.setFont(getFont());
   curProg.setFont(getFont());
   fullProg.setFont(getFont());
   highPanel.add(statLabel);
   highPanel.add(curProg);
   highPanel.add(fullProg);
   subPanel.add(nPanel, BorderLayout.NORTH);
   subPanel.add(sPanel, BorderLayout.SOUTH);
   subPanel.add(wPanel, BorderLayout.WEST);
   subPanel.add(ePanel, BorderLayout.EAST);
   subPanel.add(highPanel, BorderLayout.CENTER);  
   lowPanel.setFont(getFont());
   cancelBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { stopProcess(); } });
   cancelBtn.setFont(getFont());
   lowPanel.add(cancelBtn);
   rqDialog = new RewriteQuestionDialog(this,
     fmgr.getCopyQuestion().split("#")[0], fmgr.getCopyQuestion().split("#")[2],
     fmgr.getCopyQuestion().split("#")[3], fmgr.getCopyQuestion().split("#")[4],
     fmgr.getCopyQuestion().split("#")[5], fmgr.getCopyQuestion().split("#")[6]);
   getContentPane().add(subPanel, BorderLayout.CENTER);
   getContentPane().add(lowPanel, BorderLayout.SOUTH);
  } catch (Exception e) { fx.Main.killApp("progress", "error while initializing frame"); }
 }
 // start process
 public void startProcess(UnixFile[] targets, UnixFile destDir, boolean copy, boolean delete)
 {
  // setup new process
  cwd = fmgr.getCwd(); // remember cwd
  setTitle(" ");
  statLabel.setText("..."); // set status label
  cancelBtn.setEnabled(false); // disable cancel button
  cancelBtn.setText(" ");
  setVisible(true);  // show frame
  thread = new ProgressThread(this, targets, destDir, copy, delete);
  thread.start(); // run thread
  cancelBtn.setEnabled(true); // enable cancel button
 }
 // stop process
 private void stopProcess()
 {
  if (thread != null) // if thread is running
  thread.kill(true); // stop and check damaged files
 }
 // get cancel button, status label, progress bars, file manager, cwd
 public JButton getButton() { return cancelBtn; }
 public JLabel getStatus() { return statLabel; }
 public JProgressBar getCurrentProgress() { return curProg; }
 public JProgressBar getFullProgress() { return fullProg; }
 public FileManager getManager() { return fmgr; }
 public RewriteQuestionDialog getQuestionDialog() { return rqDialog; }
 public String getCwd() { return cwd; }
}
