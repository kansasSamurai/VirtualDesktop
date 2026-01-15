package org.jwellman.swing_applets;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/**
 * Original intent is to be a small widget that allows
 * the user to view a JComponent's location and size.
 * Buttons/actions allow the user to modify the location.
 * (size modification may be added later but I don't have a use case for that yet)
 * 
 * @author Rick Wellman
 *
 */
@SuppressWarnings("serial")
public class ComponentPlacer extends JPanel {

    private JTextField txtX = new JTextField();
    private JTextField txtY = new JTextField();
    private JTextField txtW = new JTextField();
    private JTextField txtH = new JTextField();
    private JCheckBox multiplier = new JCheckBox("");
    
    private JButton up;
    private JButton down;
    private JButton left;
    private JButton right;
    
    private Action upAction, downAction, leftAction, rightAction;
    
    private Border empty = BorderFactory.createEmptyBorder(2,1,2,1);
    
    private JComponent target;
    private JComponent repaintTarget;
    
    public ComponentPlacer() {
        this.setLayout(new BorderLayout());

        // Create the actions  
        upAction = new UpAction(  "^", null, "Up", new Integer(KeyEvent.VK_UP));
        downAction = new DownAction("d", null, "Down", new Integer(KeyEvent.VK_DOWN));
        leftAction = new LeftAction("<", null, "Left", new Integer(KeyEvent.VK_LEFT));
        rightAction = new RightAction(">", null, "Right", new Integer(KeyEvent.VK_RIGHT));
        
        up = new JButton(upAction);
        down = new JButton(downAction);
        left = new JButton(leftAction);
        right = new JButton(rightAction);
        
        JComponent x,y;
        
        x = y = new JPanel ( new GridLayout(2,3) ); 
        this.add(y, BorderLayout.EAST);
        y = new JLabel(""); x.add(y);
        y = up; x.add(y);
        y = multiplier; x.add(y);
        y = left; x.add(y);
        y = down; x.add(y);
        y = right; x.add(y);

        x = y = new JPanel( new GridLayout(2,4) );  
        this.add(y, BorderLayout.WEST);

        y = decorate(txtX); x.add(y);
        y = decorate(txtY); x.add(y);
        y = decorate(txtW); x.add(y);
        y = decorate(txtH); x.add(y);

        y = decorate(new JLabel("x-pos")); x.add(y);
        y = decorate(new JLabel("y-pos")); x.add(y);
        y = decorate(new JLabel("width")); x.add(y);
        y = decorate(new JLabel("height")); x.add(y);

    }

    public class UpAction extends BaseAction {
        public UpAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon, desc, mnemonic);
        }
        public void actionPerformed(ActionEvent e) {
            Point p = target.getLocation();
            int to = p.y - (multiplier.isSelected() ? 10 : 1);
            target.setLocation(p.x, to);
            txtY.setText(Integer.toString(to));
            if (repaintTarget != null) repaintTarget.repaint();
        }
    }
    
    public class DownAction extends BaseAction {
        public DownAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon, desc, mnemonic);
        }
        public void actionPerformed(ActionEvent e) {
            Point p = target.getLocation();
            int to = p.y + (multiplier.isSelected() ? 10 : 1);
            target.setLocation(p.x, to);
            txtY.setText(Integer.toString(to));
            if (repaintTarget != null) repaintTarget.repaint();
        }
    }
    
    public class LeftAction extends BaseAction {
        public LeftAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon, desc, mnemonic);
        }
        public void actionPerformed(ActionEvent e) {
            Point p = target.getLocation();
            int to = p.x - (multiplier.isSelected() ? 10 : 1);
            target.setLocation(to, p.y);
            txtX.setText(Integer.toString(to));
            if (repaintTarget != null) repaintTarget.repaint();
        }
    }
    
    public class RightAction extends BaseAction {
        public RightAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon, desc, mnemonic);
        }
        public void actionPerformed(ActionEvent e) {
            Point p = target.getLocation();
            int to = p.x + (multiplier.isSelected() ? 10 : 1);
            target.setLocation(to, p.y);
            txtX.setText(Integer.toString(to));
            if (repaintTarget != null) repaintTarget.repaint();
        }
    }
    
    public class BaseAction extends AbstractAction {
        public BaseAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        @Override
        public void actionPerformed(ActionEvent e) {}        
    }

    private JTextField decorate(JTextField text) {
        text.setBorder(empty);
        return text;
    }

    private JLabel decorate(JLabel label) {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        
        return label;
    }

    /**
     * @return the target
     */
    public JComponent getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(JComponent target) {
        this.target = target;
    }

    /**
     * @return the repaintTarget
     */
    public JComponent getRepaintTarget() {
        return repaintTarget;
    }

    /**
     * @param repaintTarget the repaintTarget to set
     */
    public void setRepaintTarget(JComponent repaintTarget) {
        this.repaintTarget = repaintTarget;
    }
    
}
