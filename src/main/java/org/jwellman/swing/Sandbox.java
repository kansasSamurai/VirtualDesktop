package org.jwellman.swing;

import java.awt.Color ;
import java.awt.Dimension ;
import java.awt.Font ;
import java.awt.GradientPaint ;
import java.awt.Graphics ;
import java.awt.Graphics2D ;
import java.awt.RenderingHints ;

import javax.swing.JComponent;

/**
 * This is just a "dummy" component that displays some hardcoded user interface concepts.
 * Hopefully, real components will emerge from this.
 * 
 * @author rwellman
 *
 * How to demo inside JPAD:

import org.jwellman.swing.Sandbox;

public class Demo extends JPanel {

    public Demo() {
        super(new GridBagLayout());

        Sandbox demo = new Sandbox();
        this.add(demo); //, BorderLayout.CENTER);
    }

}

jvd.a = DesktopManager.get().createVApp(new Demo(), "Demo");

 */
public class Sandbox extends JComponent {

    private static final long serialVersionUID = 1L ;

    private int height, width;

    private static final Color myblue = new Color(0x004892);
    private static final Color darkblue = new Color(0x003770);
    private static final Color lightblue = new Color(0x69E2FF);
    private static final Color midnightblue = new Color(0x012D5C);
    private static final Color ledgreen = new Color(0x12FF00);
    private static final Color ledwarning = new Color(0xFFB901);
    
    private static final Font MONO_LARGE = new Font("Consolas", Font.PLAIN, 18);
    private static final Font MONO_SMALL = new Font("Consolas", Font.BOLD, 12);
    
    // This is not intended to be made public; a flyweight to simply avoid the overhead of a new object each time.
    private Dimension dimension = new Dimension();
    
    public Sandbox() {
        this.width = 225 ;
        this.height = 110;
        this.dimension.setSize(this.width, this.height);
        this.setBackground(myblue);
        this.setFont(MONO_LARGE);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(this.getFont());

        //g2.setColor(this.getBackground());
        g2.setPaint(new GradientPaint(0, 50, darkblue, 112, 50, myblue, true));
        g2.fillRect(0, 0, this.width, this.height);
        
        g2.setPaint(new GradientPaint(0, 1, myblue, 112, 1, lightblue, true));
        g2.fillRect(0, 6, this.width, 1);
        
        g2.setColor(lightblue);
        g2.drawString("TUE 11 NOV 2020", 10, 30);

        g2.setColor(Color.white);
        g2.drawString("10:51 PM", 10, 50);

        int x=45, y=81;
        g2.setColor(Color.white); g2.setFont(MONO_SMALL);
        g2.drawString("RAM", 10, 90);
        
        g2.setColor(ledgreen);
        for (int n=0; n<5; n++) {
            g2.fillRect(x += 5, y, 4, 10);            
        }

        g2.setColor(midnightblue);
        for (int n=0; n<10; n++) {
            g2.fillRect(x += 5, y, 4, 10);            
        }
        
        x=45; y=95;
        g2.setColor(Color.white); g2.setFont(MONO_SMALL);
        g2.drawString("JVM", 10, 102);
        
        g2.setColor(ledgreen);
        for (int n=0; n<10; n++) {
            g2.fillRect(x += 5, y, 4, 10);            
        }

        g2.setColor(ledwarning);
        for (int n=0; n<3; n++) {
            g2.fillRect(x += 5, y, 4, 10);            
        }

        g2.setColor(midnightblue);
        for (int n=0; n<2; n++) {
            g2.fillRect(x += 5, y, 4, 10);            
        }

    }
    
    @Override
    public Dimension getPreferredSize() {
        return this.dimension;
    }
    
}
