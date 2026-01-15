package org.jwellman.demo;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JApplet;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/* 
 * This applet renders an ellipse overlapping a rectangle with the compositing rule and 
 * alpha value selected by the user.
*/

public class Composite extends JApplet implements ItemListener {

    CompPanel comp;
    JLabel alphaLabel, rulesLabel;
    JComboBox<String> alphas;
    JComboBox<String> rules;
    String alpha = "1.0";
    int rule = 0;

    private static final long serialVersionUID = 1L;

    // Initializes the layout of the components.
    public void init_original() {
        GridBagLayout layOut = new GridBagLayout();
        getContentPane().setLayout(layOut);

        GridBagConstraints l = new GridBagConstraints();
        l.weightx = 1.0;
        l.fill = GridBagConstraints.BOTH;
        l.gridwidth = GridBagConstraints.RELATIVE;
        alphaLabel = new JLabel();
        alphaLabel.setText("Alphas");
        Font newFont = getFont().deriveFont(1);
        alphaLabel.setFont(newFont);
        alphaLabel.setHorizontalAlignment(JLabel.CENTER);
        layOut.setConstraints(alphaLabel, l);
        getContentPane().add(alphaLabel);   


        l.gridwidth = GridBagConstraints.REMAINDER;
        rulesLabel = new JLabel();
        rulesLabel.setText("Rules");
        newFont = getFont().deriveFont(1);
        rulesLabel.setFont(newFont);
        rulesLabel.setHorizontalAlignment(JLabel.CENTER);
        layOut.setConstraints(rulesLabel, l);
        getContentPane().add(rulesLabel);   

        GridBagConstraints a = new GridBagConstraints();
        a.gridwidth = GridBagConstraints.RELATIVE;
        a.weightx = 1.0;
        a.fill = GridBagConstraints.BOTH;
        alphas = new JComboBox<>();
        layOut.setConstraints(alphas, a);
        alphas.addItem("1.0");
        alphas.addItem("0.75");
        alphas.addItem("0.50");
        alphas.addItem("0.25");
        alphas.addItem("0.0");
        alphas.addItemListener(this);
        getContentPane().add(alphas);

        a.gridwidth = GridBagConstraints.REMAINDER;
        rules = new JComboBox<>();
        layOut.setConstraints(rules, a);
        rules.addItem("SRC");
        rules.addItem("DST_IN");
        rules.addItem("DST_OUT");
        rules.addItem("DST_OVER");
        rules.addItem("SRC_IN");
        rules.addItem("SRC_OVER");
        rules.addItem("SRC_OUT");
        rules.addItem("CLEAR");
        rules.addItemListener(this);
        getContentPane().add(rules);

        GridBagConstraints fC = new GridBagConstraints(); 
        fC.fill = GridBagConstraints.BOTH;
        fC.weightx = 1.0;
        fC.weighty = 1.0;
        fC.gridwidth = GridBagConstraints.REMAINDER;
        comp = new CompPanel();
        layOut.setConstraints(comp, fC);
        getContentPane().add(comp); 

        validate();
    }

    public void init() {
        getContentPane().setLayout(new BorderLayout());

        JPanel north = new JPanel(new GridLayout(2,2));
        this.add(north, BorderLayout.NORTH);

        Font newFont = getFont().deriveFont(Font.BOLD);

        alphaLabel = new JLabel();
        alphaLabel.setText("Alphas");
        alphaLabel.setFont(newFont);
        alphaLabel.setHorizontalAlignment(JLabel.CENTER);
        north.add(alphaLabel);

        rulesLabel = new JLabel();
        rulesLabel.setText("Rules");
        rulesLabel.setFont(newFont);
        rulesLabel.setHorizontalAlignment(JLabel.CENTER);
        north.add(rulesLabel);

        alphas = new JComboBox<>();
        alphas.addItem("1.0");
        alphas.addItem("0.90");
        alphas.addItem("0.80");
        alphas.addItem("0.70");
        alphas.addItem("0.60");
        alphas.addItem("0.50");
        alphas.addItem("0.40");
        alphas.addItem("0.30");
        alphas.addItem("0.20");
        alphas.addItem("0.10");
        alphas.addItem("0.0");
        alphas.addItemListener(this);
        north.add(alphas);

        rules = new JComboBox<>();
        rules.addItem("SRC");
        rules.addItem("DST_IN");
        rules.addItem("DST_OUT");
        rules.addItem("DST_OVER");
        rules.addItem("SRC_IN");
        rules.addItem("SRC_OVER");
        rules.addItem("SRC_OUT");
        rules.addItem("CLEAR");
        rules.addItemListener(this);
        north.add(rules);

        comp = new CompPanel();
        this.add(comp, BorderLayout.CENTER);

        validate();
    }

    /*
     * Detects a change in either of the Choice components.  Resets the variable corresponding
     * to the Choice whose state is changed.  Invokes changeRule in CompPanel with the current
     * alpha and composite rules.
    */
    public void itemStateChanged(ItemEvent e){
        if ( e.getStateChange() != ItemEvent.SELECTED ) {
            return;
        }

        Object choice = e.getSource();
        if ( choice == alphas ) {
            alpha = (String)alphas.getSelectedItem();
        } else {
            rule = rules.getSelectedIndex();
        }
        comp.changeRule(alpha, rule);
    }

    public static void main(String s[]) {
        JFrame f = new JFrame("Composite");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        JApplet applet = new Composite();
        f.getContentPane().add("Center", applet);
        applet.init();
        f.pack();
        f.setSize(new Dimension(300,300));
        f.setVisible(true);
    }
}


class CompPanel extends JPanel {

    private float alpha = 1.0f;
    private AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
    private Color srcColor = new Color(0.0f, 0.0f, 1.0f, 1.0f);
    private Color dstColor = new Color(1.0f, 0.0f, 0.0f, 1.0f);

    private static final long serialVersionUID = 1L;

    public CompPanel(){}

    // Resets the alpha and composite rules with selected items.    
    public void changeRule(String a, int rule) {
        alpha = Float.valueOf(a).floatValue();
        ac = AlphaComposite.getInstance(getRule(rule), alpha);
        repaint();
    }

    // Gets the requested compositing rule.
    public int getRule(int rule){
        int alphaComp = 0;
        switch ( rule ) {
        case 0: alphaComp = AlphaComposite.SRC; break;
        case 1: alphaComp = AlphaComposite.DST_IN; break;
        case 2: alphaComp = AlphaComposite.DST_OUT; break;
        case 3: alphaComp = AlphaComposite.DST_OVER; break;
        case 4: alphaComp = AlphaComposite.SRC_IN; break;
        case 5: alphaComp = AlphaComposite.SRC_OVER; break;
        case 6: alphaComp = AlphaComposite.SRC_OUT; break;
        case 7: alphaComp = AlphaComposite.CLEAR; break;
        }
        return alphaComp;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent( g );

        Graphics2D g2 = (Graphics2D) g;

        Dimension d = getSize();
        int w = d.width;
        int h = d.height; 

        // Clears the previously drawn image.
        g2.setColor(Color.white);
        g2.fillRect(0, 0, d.width, d.height);

        // Creates the buffered image.
        int rectx = w/4;
        int recty = h/4;
        BufferedImage buffImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gbi = buffImg.createGraphics();

        // Draws the rectangle and ellipse into the buffered image.
        gbi.setColor(srcColor);
        gbi.fill(new Rectangle2D.Double(rectx, recty, 150, 100));

        gbi.setColor(dstColor);
        gbi.drawRect(rectx+rectx/2,recty+recty/2,150,100);
        gbi.setComposite(ac);
        gbi.fill(new Ellipse2D.Double(rectx+rectx/2,recty+recty/2,150,100));

        // Draws the buffered image.
        g2.drawImage(buffImg, null, 0, 0);
        gbi.dispose();
    }

}
