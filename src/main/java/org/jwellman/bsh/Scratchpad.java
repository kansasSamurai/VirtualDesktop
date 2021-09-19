package org.jwellman.bsh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.jwellman.swing.icon.ColorIcon;
import org.jwellman.swing.jbutton.RolloverButton;
import org.jwellman.swing.jpanel.RestrictedHeightPanel;

/**
 * This class is just used to assist with writing BSH scripts
 * and thus it contains no real logic.  Just used within an IDE
 * to use the IDE for code completion, etc. and then copy/paste
 * code into a beanshell script.
 * 
 * @author rwellman
 *
 */
public class Scratchpad {

    public Scratchpad() {
        int a = BufferedImage.TYPE_INT_ARGB;
        //ImageIO.write(im, formatName, output);
        new JButton("Icon Color", new ColorIcon(Color.black, 14));
        UIManager.get("Tree.collapsedIcon");
    }
    
    public Scratchpad(int a) {  
        JFrame f= new JFrame();

        JDialog d = new JDialog(f , "Dialog Example", true);  

        JButton b = new JButton ("OK");  
        b.addActionListener ( new ActionListener() {  
            public void actionPerformed( ActionEvent e ) {  
                d.setVisible(false);  
            }  
        });  
        
        d.setLayout( new FlowLayout() );  
        d.add( new JLabel ("Click button to continue."));  
        d.add(b);   
        d.setSize(300,300);    
        d.setVisible(true);  
    }  

    private static final Font VERDANA = new Font("Verdana", Font.PLAIN, 10);    
    private static final Font VERDANA2 = new Font("Verdana", Font.PLAIN, 14);    
    private static final Font VERDANA3 = new Font("Verdana", Font.BOLD, 18);    
    private static final Font LABEL_FONT = new Font("Calibri", Font.BOLD, 12);

    private static final Color TEAL = new Color(0x078e8e);
    private static final Color DARK_TEAL = new Color(0x016565);
    private static final Color DARK_GRAY = new Color(0x657b83);

    private static final Border CBUTTON_BORDER = BorderFactory.createEmptyBorder(1,1,1,1); //(2,2,2,2);
    
    protected Component createEasternComponent() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));     
        
        // Reusable object refs
        JPanel panel, panel2;
        JLabel label;
        JButton button;

        FlowLayout flow = new FlowLayout(FlowLayout.CENTER, 0, 0);

        panel = panel(false);
        panel.setLayout(flow);
        panel.setBackground(DARK_TEAL);     
            label = label("September 2019");
            label.setForeground(Color.WHITE);
            label.setFont(VERDANA);
            panel.add(label);
        container.add(panel);
        
        panel = panel(false);
        panel.setLayout(flow);
        panel.setBackground(TEAL);
            label = label("Monday");
            label.setForeground(Color.WHITE);
            label.setFont(VERDANA2);
            panel.add(label);
        container.add(panel);
                
        panel = panel(false);
        panel.setLayout(flow);
        panel.setBackground(TEAL);
            label = label("4th");
            label.setForeground(Color.WHITE);
            label.setFont(VERDANA3);
            panel.add(label);
        container.add(panel);
                
        panel = panel(false);
        panel.setBackground(TEAL);
        panel.setLayout(new BorderLayout());
//          label = l("[]");
//          label.setForeground(Color.WHITE);
//          label.setFont(VERDANA2);
//          panel.add(label, BorderLayout.WEST);
            button = b2("[]"); panel.add(button, BorderLayout.WEST);

            label = label("[]");
            label.setForeground(Color.WHITE);
            label.setFont(VERDANA2);
            panel.add(label, BorderLayout.EAST);
        
            panel2 = new JPanel(new GridBagLayout());
            panel2.setOpaque(false);
                label = label("12:00 PM");
                label.setForeground(Color.LIGHT_GRAY);
                label.setFont(VERDANA);
                panel2.add(label);
            panel.add(panel2, BorderLayout.CENTER);
        
        container.add(panel);
                
        panel = panel(false);
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BorderLayout());
            label = label("<");
            // label.setForeground(Color.WHITE);
            label.setFont(VERDANA2);
            panel.add(label, BorderLayout.WEST);

            label = label(">");
            //label.setForeground(Color.WHITE);
            label.setFont(VERDANA2);
            panel.add(label, BorderLayout.EAST);
        
            panel2 = new JPanel(new GridBagLayout());
            panel2.setOpaque(false);
                label = label("September 2019");
                label.setFont(VERDANA);
                panel2.add(label);
            panel.add(panel2, BorderLayout.CENTER);
        
        container.add(panel);
        
        panel = panel(false);
        panel.setBackground(Color.WHITE);
        panel.setLayout(new GridLayout(1,7));
            button = b2("Sun"); button.setEnabled(false); panel.add(button);
            button = b2("Mon"); button.setEnabled(false); panel.add(button);
            button = b2("Tue"); button.setEnabled(false); panel.add(button);
            button = b2("Wed"); button.setEnabled(false); panel.add(button);
            button = b2("Thu"); button.setEnabled(false); panel.add(button);
            button = b2("Fri"); button.setEnabled(false); panel.add(button);
            button = b2("Sat"); button.setEnabled(false); panel.add(button);
        
        container.add(panel);
        
        for (int row = 0; row < 5; row++) {
            panel = panel(false);
            panel.setBackground(Color.WHITE);           
            panel.setLayout(new GridLayout(1,7));
            for (int col = 1; col < 8; col++) {
                final int day = row*7+col;
                final String s = (day < 32) ? ("" + day) : "";
                button = b2(s); panel.add(button);
                if (day > 31) button.setEnabled(false);
            }
            
            container.add(panel);           
        }
        
        return container;
    }

    protected JLabel label(String text) {
        final JLabel label = new JLabel(text);
        if (true) {
            label.setFont(LABEL_FONT);          
            label.setForeground(DARK_GRAY);
        }
        
        return label;
    }
    
    private JPanel panel(boolean padleft) {
        final JPanel p = new RestrictedHeightPanel(); // JPanel();
        
        if (true) {
            p.setBackground(Color.WHITE);
            if (padleft)
                p.setBorder(BorderFactory.createEmptyBorder(0,15,0,0));         
        }
        
        return p;       
    }
    
    private JButton b2(String string) {
        Color d = (Color) UIManager.getDefaults().get("Button.disabledText");// .put("Button.disabledText")
        RolloverButton b = new RolloverButton(string);
        b.setFont(VERDANA);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setBorder(CBUTTON_BORDER);
        b.setRollColor(d);
//      Insets inset = b.getBorder().getBorderInsets(b);
//      System.out.println(inset.toString());
        return b;
    }

}

class Calendar extends JPanel {
    
    public final Font VERDANA = new Font("Verdana", Font.PLAIN, 10);    
    public final Font VERDANA2 = new Font("Verdana", Font.PLAIN, 14);    
    public final Font VERDANA3 = new Font("Verdana", Font.BOLD, 18);    
    public final Font LABEL_FONT = new Font("Calibri", Font.BOLD, 12);

    public final Color TEAL = new Color(0x078e8e);
    public final Color DARK_TEAL = new Color(0x016565);
    public final Color DARK_GRAY = new Color(0x657b83);

    public final Border CBUTTON_BORDER = BorderFactory.createEmptyBorder(1,1,1,1); //(2,2,2,2);
    
    public Calendar() {
        JPanel container = this;
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));     
        
        // Reusable object refs
        JPanel panel, panel2;
        JLabel label;
        JButton button;

        FlowLayout flow = new FlowLayout(FlowLayout.CENTER, 0, 0);

        panel = panel(false);
        panel.setLayout(flow);
        panel.setBackground(DARK_TEAL);     
            label = label("September 2019");
            label.setForeground(Color.WHITE);
            label.setFont(VERDANA);
            panel.add(label);
        container.add(panel);
        
        panel = panel(false);
        panel.setLayout(flow);
        panel.setBackground(TEAL);
            label = label("Monday");
            label.setForeground(Color.WHITE);
            label.setFont(VERDANA2);
            panel.add(label);
        container.add(panel);
                
        panel = panel(false);
        panel.setLayout(flow);
        panel.setBackground(TEAL);
            label = label("4th");
            label.setForeground(Color.WHITE);
            label.setFont(VERDANA3);
            panel.add(label);
        container.add(panel);
                
        panel = panel(false);
        panel.setBackground(TEAL);
        panel.setLayout(new BorderLayout());
//          label = l("[]");
//          label.setForeground(Color.WHITE);
//          label.setFont(VERDANA2);
//          panel.add(label, BorderLayout.WEST);
            button = b2("[]"); panel.add(button, BorderLayout.WEST);

            label = label("[]");
            label.setForeground(Color.WHITE);
            label.setFont(VERDANA2);
            panel.add(label, BorderLayout.EAST);
        
            panel2 = new JPanel(new GridBagLayout());
            panel2.setOpaque(false);
                label = label("12:00 PM");
                label.setForeground(Color.LIGHT_GRAY);
                label.setFont(VERDANA);
                panel2.add(label);
            panel.add(panel2, BorderLayout.CENTER);
        
        container.add(panel);
                
        panel = panel(false);
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BorderLayout());
            label = label("<");
            // label.setForeground(Color.WHITE);
            label.setFont(VERDANA2);
            panel.add(label, BorderLayout.WEST);

            label = label(">");
            //label.setForeground(Color.WHITE);
            label.setFont(VERDANA2);
            panel.add(label, BorderLayout.EAST);
        
            panel2 = new JPanel(new GridBagLayout());
            panel2.setOpaque(false);
                label = label("September 2019");
                label.setFont(VERDANA);
                panel2.add(label);
            panel.add(panel2, BorderLayout.CENTER);
        
        container.add(panel);
        
        panel = panel(false);
        panel.setBackground(Color.WHITE);
        panel.setLayout(new GridLayout(1,7));
            button = b2("Sun"); button.setEnabled(false); panel.add(button);
            button = b2("Mon"); button.setEnabled(false); panel.add(button);
            button = b2("Tue"); button.setEnabled(false); panel.add(button);
            button = b2("Wed"); button.setEnabled(false); panel.add(button);
            button = b2("Thu"); button.setEnabled(false); panel.add(button);
            button = b2("Fri"); button.setEnabled(false); panel.add(button);
            button = b2("Sat"); button.setEnabled(false); panel.add(button);
        
        container.add(panel);
        
        for (int row = 0; row < 5; row++) {
            panel = panel(false);
            panel.setBackground(Color.WHITE);           
            panel.setLayout(new GridLayout(1,7));
            for (int col = 1; col < 8; col++) {
                final int day = row*7+col;
                final String s = (day < 32) ? ("" + day) : "";
                button = b2(s); panel.add(button);
                if (day > 31) button.setEnabled(false);
            }
            
            container.add(panel);           
        }

    }

    protected JLabel label(String text) {
        final JLabel label = new JLabel(text);
        if (true) {
            label.setFont(LABEL_FONT);          
            label.setForeground(DARK_GRAY);
        }
        
        return label;
    }
    
    private JPanel panel(boolean padleft) {
        final JPanel p = new RestrictedHeightPanel(); // JPanel();
        
        if (true) {
            p.setBackground(Color.WHITE);
            if (padleft)
                p.setBorder(BorderFactory.createEmptyBorder(0,15,0,0));         
        }
        
        return p;       
    }
    
    private JButton b2(String string) {
        Color d = (Color) UIManager.getDefaults().get("Button.disabledText");// .put("Button.disabledText")
        RolloverButton b = new RolloverButton(string);
        b.setFont(VERDANA);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setBorder(CBUTTON_BORDER);
        b.setRollColor(d);
//      Insets inset = b.getBorder().getBorderInsets(b);
//      System.out.println(inset.toString());
        return b;
    }

}
