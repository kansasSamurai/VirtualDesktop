package org.jwellman.demo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

class CSurface extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int rules[] = {
            AlphaComposite.SRC,
            AlphaComposite.DST,
            AlphaComposite.SRC_ATOP,
            AlphaComposite.DST_ATOP,
            AlphaComposite.SRC_IN,
            AlphaComposite.DST_IN,
            AlphaComposite.SRC_OUT,
            AlphaComposite.DST_OUT,
            AlphaComposite.SRC_OVER,
            AlphaComposite.DST_OVER,
            AlphaComposite.CLEAR,
            AlphaComposite.XOR 
    };

    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g.create();

        for (int x = 20, y = 20, i = 0; i < rules.length; x += 60, i++) {

            BufferedImage buffImg = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gbi = buffImg.createGraphics();

            // Destination is yellow circle
            gbi.setPaint(Color.yellow);
            gbi.fillOval(15, 5, 40, 40);

            // Note:  This was re-written to compare against the following
            // post on Oracle docs; the results do not seem to match:
            // https://docs.oracle.com/javase/tutorial/2d/advanced/compositing.html

            // Apply the composite - any further painting is considered the "source"
            gbi.setComposite(AlphaComposite.getInstance(rules[i], 0.8f));

            // Source is the black square
            gbi.setPaint(Color.black);
            gbi.fillRect(0, 20, 40, 40);

            g2d.drawImage(buffImg, x, y, null);
            gbi.dispose();
        }

        g2d.dispose();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        doDrawing(g);
    }

}

public class CompositionEx extends JFrame {

    private static final long serialVersionUID = 1L;

    public CompositionEx() {

        add(new CSurface());

        setTitle("Composition");
        setSize(400, 120);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                CompositionEx ex = new CompositionEx();
                ex.setVisible(true);
            }
        });

    }

}
