package org.jwellman.demo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

class Surface extends JPanel {

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
            AlphaComposite.CLEAR
    };

    private static final long serialVersionUID = 1L;

    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g.create();

        for (int x = 20, y = 20, i = 0; i < rules.length; x += 60, i++) {

            BufferedImage buffImg = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gbi = buffImg.createGraphics();

            // Destination is black
            gbi.setPaint(Color.yellow);
            gbi.fillOval(15, 5, 40, 40);

            // Note:  This was re-written to compare against the following
            // post on Oracle docs; the results do not seem to match:
            // https://docs.oracle.com/javase/tutorial/2d/advanced/compositing.html

            // Apply the composite - any further painting is considered the "source"
            gbi.setComposite(AlphaComposite.getInstance(rules[i], 0.8f));

            // Source(yellow) is the "new" stuff drawn "onto" the existing "destination"
            gbi.setPaint(Color.black);
            gbi.fillRect(0, 20, 40, 40);

            g2d.drawImage(buffImg, x, y, null);
            gbi.dispose();
        }

        g2d.dispose();
    }

    @SuppressWarnings("unused")
    private void doDrawing_original(Graphics g) {

        Graphics2D g2d = (Graphics2D) g.create();

        for (int x = 20, y = 20, i = 0; i < rules.length; x += 60, i++) {

            AlphaComposite ac = AlphaComposite.getInstance(rules[i], 0.8f);

            BufferedImage buffImg = new BufferedImage(60, 60,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D gbi = buffImg.createGraphics();

            // Destination is blue
            gbi.setPaint(Color.blue);
            gbi.fillRect(0, 0, 40, 40);

            // Apply the composite
            gbi.setComposite(ac);

            // Source(green) is the "new" stuff drawn "onto" the "destination"
            gbi.setPaint(Color.green);
            gbi.fillRect(5, 5, 40, 40);

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

