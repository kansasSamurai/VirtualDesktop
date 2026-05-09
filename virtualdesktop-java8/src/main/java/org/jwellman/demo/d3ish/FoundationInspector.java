package org.jwellman.demo.d3ish;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class FoundationInspector extends JPanel {

    private final JPanel container = new JPanel();

    public FoundationInspector() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(new Color(245, 245, 245));
        container.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Audit Details");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 0));

        JScrollPane sp = new JScrollPane(container);
        sp.setBorder(null);
        add(title, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
    }

    public void update(BarComponent bar) {
        container.removeAll();

        // Add a series of cards based on the Bar's data
        container.add(createPropertyCard("Target Task", bar.getText(), bar.getBackground()));
        container.add(Box.createVerticalStrut(10));
        container.add(createPropertyCard("Load Factor", (int) (bar .getValue() * 100) + "%", Color.LIGHT_GRAY));
        container.add(Box.createVerticalStrut(10));
        container.add(createPropertyCard("Thread State", "VIRTUAL_THREAD_WAITING", Color.ORANGE));
        container.add(Box.createVerticalStrut(10));
        container.add(createPropertyCard("Memory Delta \uD83D\uDCBE", "+14.2 MB", new Color(46, 204, 113)));
        // this is a surrogate pair: \uD83D\uDCBE        for floppy disk U+1F4BE
        // hourglass: \u23F3
        container.revalidate();
        container.repaint();
    }

    private JPanel createPropertyCard(String label, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel lblTitle = new JLabel(label.toUpperCase());
        lblTitle.setFont(new Font("SegoeUI", Font.BOLD, 10));
        lblTitle.setForeground(Color.GRAY);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("SegoeUI", Font.PLAIN, 14));
        lblValue.setForeground(Color.DARK_GRAY);

        // Visual "Accent" strip on the left
        JPanel accentStrip = new JPanel();
        accentStrip.setPreferredSize(new Dimension(4, 0));
        accentStrip.setBackground(accent);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        card.add(accentStrip, BorderLayout.WEST);

        return card;
    }

}
