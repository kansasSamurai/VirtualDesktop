/* Copyright (c) 2001-2011, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package ext.hsqldb.util;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;

//  weconsultants@users 20041109 - original swing port
//  weconsultants@users 20050215 - version 1.8.0 -  Update: Compatbilty fix for JDK 1.3
//      - Replaced: Objects JSpinner spinnerFontSizes and SpinnerNumberModel spinnerModelSizes
//        for JComboBox fontSizesComboBox and String fontSizes[];
public class FontDialogSwing extends JDialog {

    private static boolean      isRunning   = false;

    private static final String BACKGROUND  = "Background";
    private static final String FOREGROUND  = "Foreground";
    private static final String FONTNAME_DEFAULT = "Dialog"; // previously defaultFont

    private static JComboBox    fontsComboBox;
    private static JComboBox    fontSizesComboBox;
    private static JCheckBox    cboxBold; // previously cboxBold
    private static JCheckBox    cboxItalic; // previously cboxItalic
    private static JButton      btnFgColor; // previously btnFgColor
    private static JButton      btnBgColor; // previously btnBgColor
    private static JButton      btnClose; // previously btnClose

    private static final Dimension _fontsComboBoxDimension = new Dimension(160, 25);
    private static final Dimension _spinnerDimension = new Dimension(45, 25);

    //  weconsultants@users 20050215 - Added for Compatbilty fix for JDK 1.3
    private static final String[] fontSizes = {
        "8", "9", "10", "11", "12", "13", "14", "16", "18", "24", "36"
    };

    // weconsultants@users 20050215 - Commented out for Compatbilty fix for JDK 1.3
    //  private static JSpinner           spinnerFontSizes;
    //  private static SpinnerNumberModel spinnerModelSizes;
    private static DatabaseManagerInterface fOwner;

    // TODO rethink this? to work with JPAD/JInternalFrames?
    private static final JFrame frame = new JFrame("Font Selection Dialog");

    /**
     * Create and display FontDialogSwing Dialog.
     *
     * @param owner
     */
    public static void creatFontDialog(DatabaseManagerInterface owner) {

        if (isRunning) {
            fOwner = owner;
            frame.setVisible(true);
        } else {
            isRunning = true;

            // CommonSwing.setSwingLAF(frame, CommonSwing.Native);

            fOwner = owner;

            frame.setIconImage(CommonSwing.getIcon("Frame"));
            frame.setSize(600, 100);
            CommonSwing.setFramePositon(frame);

            cboxItalic = new JCheckBox( new ImageIcon(CommonSwing.getIcon("ItalicFont")) );
            cboxItalic.putClientProperty("is3DEnabled", Boolean.TRUE);
            cboxItalic.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setStyle();
                }
            });

            cboxBold = new JCheckBox( new ImageIcon(CommonSwing.getIcon("BoldFont")) );
            cboxBold.putClientProperty("is3DEnabled", Boolean.TRUE);
            cboxBold.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setStyle();
                }
            });

            btnFgColor = new JButton( "Foreground", new ImageIcon(CommonSwing.getIcon("ColorSelection")) );
            btnFgColor.putClientProperty("is3DEnabled", Boolean.TRUE);
            btnFgColor.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setColor(FOREGROUND);
                }
            });

            btnBgColor = new JButton( "Background", new ImageIcon(CommonSwing.getIcon("ColorSelection")) );
            btnBgColor.putClientProperty("is3DEnabled", Boolean.TRUE);
            btnBgColor.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setColor(BACKGROUND);
                }
            });

            btnClose = new JButton("Close", new ImageIcon(CommonSwing.getIcon("Close")));
            btnClose.putClientProperty("is3DEnabled", Boolean.TRUE);
            btnClose.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                }
            });

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final String[] fontNames = ge.getAvailableFontFamilyNames();

            fontsComboBox = new JComboBox(fontNames);
            fontsComboBox.putClientProperty("is3DEnabled", Boolean.TRUE);
            fontsComboBox.setEditable(false);
            fontsComboBox.setSelectedItem(FONTNAME_DEFAULT);
            fontsComboBox.setMaximumSize(_fontsComboBoxDimension);
            fontsComboBox.setPreferredSize(_fontsComboBoxDimension);
            fontsComboBox.setMaximumSize(_fontsComboBoxDimension);
            fontsComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setFont();
                }
            });

            // weconsultants@users 20050215 - Added for Compatbilty fix for  JDK 1.3
            fontSizesComboBox = new JComboBox(fontSizes);
            fontSizesComboBox.putClientProperty("is3DEnabled", Boolean.TRUE);
            fontSizesComboBox.setMinimumSize(_spinnerDimension);
            fontSizesComboBox.setPreferredSize(_spinnerDimension);
            fontSizesComboBox.setMaximumSize(_spinnerDimension);
            fontSizesComboBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt) {
                    if (evt.getStateChange() == ItemEvent.SELECTED) {
                        setFontSize((String) evt.getItem());
                    }
                }
            });

            // weconsultants@users 20050215 - Commented out for Compatbilty fix for  JDK 1.3
            //            Dimension _spinnerDimension = new Dimension(50, 25);
            //            spinnerFontSizes = new JSpinner();
            //            spinnerFontSizes.putClientProperty("is3DEnabled", Boolean.TRUE);
            //            spinnerFontSizes.setMinimumSize(_spinnerDimension);
            //            spinnerFontSizes.setPreferredSize(_spinnerDimension);
            //            spinnerFontSizes.setMaximumSize(_spinnerDimension);
            //            spinnerModelSizes = new SpinnerNumberModel(12, 8, 72, 1);
            //            spinnerFontSizes.setModel(spinnerModelSizes);
            //            spinnerFontSizes.addChangeListener(new ChangeListener() {
            //                public void stateChanged(ChangeEvent e) {
            //                    setFontSize();
            //                }
            //            });

            Container contentPane = frame.getContentPane();
            contentPane.setLayout(new FlowLayout());
            contentPane.add(fontsComboBox);
            // weconsultants@users 20050215 - Commented out for Compatbilty fix for 1.3
            // contentPane.add(spinnerFontSizes);
            // weconsultants@users 20050215 - Added for Compatbilty fix for 1.3
            contentPane.add(fontSizesComboBox);
            contentPane.add(cboxBold);
            contentPane.add(cboxItalic);
            contentPane.add(btnFgColor);
            contentPane.add(btnBgColor);
            contentPane.add(btnClose);

            frame.pack();
            frame.setVisible(false);
        }
    }

    /**
     * Previous implementation moved/refactored to DatabaseManagerSwing
     * (and anything that implements the interface 'DatabaseManagerInterface'
     *
     */
    public static void setFont() {
        fOwner.setFont(fontsComboBox.getSelectedItem().toString());
    }

    /**
     * Previous implementation moved/refactored to DatabaseManagerSwing
     * (and anything that implements the interface 'DatabaseManagerInterface'
     *
     * @param inFontSize
     */
    public static void setFontSize(String inFontSize) {
        fOwner.setFontSize(inFontSize);
    }

    /**
     * Previous implementation moved/refactored to DatabaseManagerSwing
     * (and anything that implements the interface 'DatabaseManagerInterface'
     *
     * Changes the style (Bold, Italic ) of the selected text
     * by checking the style buttons
     *
     */
    public static void setStyle() {

        int style = Font.PLAIN;
        if (cboxBold.isSelected()) {
            style |= Font.BOLD;
        }
        if (cboxItalic.isSelected()) {
            style |= Font.ITALIC;
        }

        fOwner.setStyle(style);
    }

    /**
     * Displays a color chooser and Sets the selected color.
     *
     * @param inTarget
     */
    public static void setColor(String inTarget) {
        if (inTarget.equals(BACKGROUND)) {
            fOwner.setBackground();
        } else {
            fOwner.setForeground();
        }
    }

}
