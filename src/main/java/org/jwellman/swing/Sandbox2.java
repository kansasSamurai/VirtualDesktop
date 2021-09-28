package org.jwellman.swing;

import java.awt.Color ;
import java.awt.Dimension ;
import java.awt.Font ;
import java.awt.GradientPaint ;
import java.awt.Graphics ;
import java.awt.Graphics2D ;
import java.awt.RenderingHints ;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.Timer;

import org.apache.commons.lang3.RandomUtils;

/**
 * This is an animated version of Sandbox.
 * 
 * This is just a "dummy" component that displays some hardcoded user interface concepts.
 * Hopefully, real components will emerge from this.
 * 
 * @author rwellman
 *
 */
public class Sandbox2 extends JComponent implements ActionListener {

    private static final long serialVersionUID = 1L ;

    private int height, width;

    // Variables to support animation
    private int ramValue, ramMax;
    private int jvmValue, jvmMax;
    private Timer timer;

    private static final Color myblue = new Color(0x004892);
    private static final Color darkblue = new Color(0x003770);
    private static final Color lightblue = new Color(0x69E2FF);
    private static final Color midnightblue = new Color(0x012D5C);
    private static final Color ledgreen = new Color(0x12FF00);
    private static final Color ledwarning = new Color(0xFFB901);
    
    private static final Font MONO_LARGE = new Font("Consolas", Font.PLAIN, 14);
    private static final Font MONO_SMALL = new Font("Consolas", Font.BOLD, 12);
    
    // This is not intended to be made public; a flyweight to simply avoid the overhead of a new object each time.
    private Dimension dimension = new Dimension();
    
    public Sandbox2() {
        this.width = 225 ;
        this.height = 110;
        this.dimension.setSize(this.width, this.height);
        this.setBackground(myblue);
        this.setFont(MONO_LARGE);

        ramValue = jvmValue = 0;
        ramMax = RandomUtils.nextInt(0, 15);
        jvmMax = RandomUtils.nextInt(0, 15);

        // 1000 / 5 is "five steps/frame per second (1000 ms)"
        this.timer = new Timer(1000 / 20, this);
        timer.setInitialDelay(0);
        timer.start();

    }
    
    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(this.getFont());

        // ===== Gradient Background =====
        g2.setPaint(new GradientPaint(0, 50, darkblue, 112, 50, myblue, true));
        g2.fillRect(0, 0, this.width, this.height);
        
        // ===== Title Sparkle Gradient =====
        g2.setPaint(new GradientPaint(0, 1, myblue, 112, 1, lightblue, true));
        g2.fillRect(0, 6, this.width, 1);
        
        // ===== Date =====
        g2.setColor(lightblue);
        g2.drawString("TUE 11 NOV 2020", 10, 30);

        // ===== Time =====
        g2.setColor(Color.white);
        g2.drawString("10:51 PM", 10, 50);

        // ===== RAM =====
        int x=45, y=81;
        g2.setColor(Color.white); g2.setFont(MONO_SMALL);
        g2.drawString("RAM", 10, 90);
                
        for (int n = 0; n < 15; n++) {
            if (n < ramValue) {
                g2.setColor(n < 10 ? ledgreen : ledwarning);
            } else {
                g2.setColor(midnightblue);
            }
            g2.fillRect(x += 5, y, 4, 10);
        }
        if (++ramValue > ramMax) {
            ramValue = 0;
            ramMax = RandomUtils.nextInt(0, 15);
        }
        
        // ===== JVM Memory =====
        x=45; y=95;
        g2.setColor(Color.white); g2.setFont(MONO_SMALL);
        g2.drawString("JVM", 10, 102);

        for (int n = 0; n < 15; n++) {
            if (n < jvmValue) {
                g2.setColor(n < 10 ? ledgreen : ledwarning);
            } else {
                g2.setColor(midnightblue);
            }
            g2.fillRect(x += 5, y, 4, 10);
        }
        if (++jvmValue > jvmMax) {
            jvmValue = 0;
            jvmMax = RandomUtils.nextInt(0, 15);
        }
        

        g2.dispose();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return this.dimension;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
        this.repaint();
    }

}
