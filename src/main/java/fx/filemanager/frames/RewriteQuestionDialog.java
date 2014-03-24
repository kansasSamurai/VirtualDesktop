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
// rewrite question dialog class
public class RewriteQuestionDialog extends JDialog
{
 // return options
 public static final int YES_OPTION = 1, YES_ALL_OPTION = 2,
   NO_OPTION = 3, NO_ALL_OPTION = 4, CANCEL_OPTION = 5;
 // params
 private final Dimension ROOT_SIZE = new Dimension(400, 100);
 private final int COMP_HGAP = 10, COMP_VGAP = 10;
 private int returnOption;
 // components
 JLabel mesgLabel = new JLabel();
 JButton yesBtn = new JButton(), yesAllBtn = new JButton(),
   noBtn = new JButton(), noAllBtn = new JButton(), cancelBtn = new JButton();
 // construct question dialog
 public RewriteQuestionDialog(JFrame owner, String titleOption, String yesOption,
   String yesAllOption, String noOption, String noAllOption, String cancelOption)
 {
  // create modal dialog with frame owner and
  super(owner, true);
  // setup components
  addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { returnOption = CANCEL_OPTION; setVisible(false); } } );
  setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
  setFont(owner.getFont());
  setLayout(new BorderLayout());
  setResizable(false);
  setTitle(titleOption);
  setSize(ROOT_SIZE);
  setLocation((Toolkit.getDefaultToolkit().getScreenSize().width / 2)
    - (getWidth() / 2), (Toolkit.getDefaultToolkit().getScreenSize().height
    / 2) - (getHeight() / 2) );
  JPanel highPanel = new JPanel(new BorderLayout());
  JPanel nPanel = new JPanel(), sPanel = new JPanel(),
    ePanel = new JPanel(), wPanel = new JPanel();
  nPanel.setSize(COMP_HGAP, COMP_VGAP);
  sPanel.setSize(COMP_HGAP, COMP_VGAP);
  ePanel.setSize(COMP_HGAP, COMP_VGAP);
  wPanel.setSize(COMP_HGAP, COMP_VGAP);
  mesgLabel.setFont(getFont());
  highPanel.add(nPanel, BorderLayout.NORTH);
  highPanel.add(sPanel, BorderLayout.SOUTH);
  highPanel.add(ePanel, BorderLayout.EAST);
  highPanel.add(wPanel, BorderLayout.WEST);
  highPanel.add(mesgLabel, BorderLayout.CENTER);
  JPanel lowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
  yesBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { returnOption = YES_OPTION; setVisible(false); } } );
  yesBtn.setFont(getFont());
  yesBtn.setText(yesOption);
  yesAllBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { returnOption = YES_ALL_OPTION; setVisible(false); } } );
  yesAllBtn.setFont(getFont());
  yesAllBtn.setText(yesAllOption);
  noBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { returnOption = NO_OPTION; setVisible(false); } } );
  noBtn.setFont(getFont());
  noBtn.setText(noOption);
  noAllBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { returnOption = NO_ALL_OPTION; setVisible(false); } } );
  noAllBtn.setFont(getFont());
  noAllBtn.setText(noAllOption);
  cancelBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { returnOption = CANCEL_OPTION; setVisible(false); } } );
  cancelBtn.setFont(getFont());
  cancelBtn.setText(cancelOption);
  lowPanel.add(yesBtn);
  lowPanel.add(yesAllBtn);
  lowPanel.add(noBtn);
  lowPanel.add(noAllBtn);
  lowPanel.add(cancelBtn);
  add(highPanel, BorderLayout.CENTER);
  add(lowPanel, BorderLayout.SOUTH);
 }
 // show question dialog
 public void showQuestionDialog(String message)
 {
  mesgLabel.setText(message);
  returnOption = 0;
  setVisible(true);
 }
 // return return option
 public int getReturnOption() { return returnOption; }
}
