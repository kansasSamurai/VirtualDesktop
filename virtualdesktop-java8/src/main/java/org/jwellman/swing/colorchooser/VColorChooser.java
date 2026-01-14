package org.jwellman.swing.colorchooser;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * An extension of JColorChooser with the following features:
 * 1) Replaces the preview panel with a custom panel consisting of a 
 *    JButton on a background matching the selected color.
 *    I think this was simply a demonstration that custom preview
 *    panels are inherently possible but it does also provide a
 *    quick and dirty example of how a button might look on a particular
 *    background with the current look and feel.
 * 
 * Note:  this OOOLLLLLD bug report ( https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4200976 )
 * seems to indicate that a JComponent registered via setPreviewPanel() will be automatically 
 * registered with a default change listener that will call setForeground() on that JComponent.
 * I will have to test that also (note that I'm guessing it would be anecdotally true
 * that most custom preview panel JComponents will actually be JPanels but technically
 * they don't have to be).
 * 
 * @author rwellman
 *
 */
public class VColorChooser extends JColorChooser implements ActionListener {

	private static final long serialVersionUID = 8947610053882835763L;

    private JPanel pnlPreview = new JPanel(new GridBagLayout());
    private JButton button = new JButton("Click to add...");

    public VColorChooser() {
        super();

        button.addActionListener(this);

        pnlPreview.setOpaque(true);
        pnlPreview.setBorder(BorderFactory.createEmptyBorder(5, 30, 5, 30));
        pnlPreview.add(button);
        this.setPreviewPanel(pnlPreview);

        this.getSelectionModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                final Color newColor = getColor();
                pnlPreview.setBackground(newColor);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.setMostRecentColor(this.getColor());
    }

    // Implement a custom property to be used with TransferHandler(s)
    // and JComponent(s) that also have this custom property.

    public Color getDndColor() {
        return this.getColor();
    }

    public void setDndColor(Color c) {
        this.setColor(c);
    }

	/**
	 * Uses reflection to expose the setMostRecentColor(Color c) method on the
	 * DefaultSwatchChooserPanel (which updates the recentSwatchPanel).
	 * 
	 * @param c
	 */
    public void setMostRecentColor(Color c) {
        for (AbstractColorChooserPanel p : this.getChooserPanels()) {
            if (p.getClass().getSimpleName().equals("DefaultSwatchChooserPanel")) {

                Field recentPanelField;
                try {
                    recentPanelField = p.getClass().getDeclaredField("recentSwatchPanel");
                    recentPanelField.setAccessible(true);

                    final Object recentPanel = recentPanelField.get(p);
                    final Method recentColorMethod = recentPanel.getClass().getMethod("setMostRecentColor", Color.class);
                    recentColorMethod.setAccessible(true);
                    recentColorMethod.invoke(recentPanel, c);

                    break;
                } catch (NoSuchFieldException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } // end if
        } // end for

    }

}
