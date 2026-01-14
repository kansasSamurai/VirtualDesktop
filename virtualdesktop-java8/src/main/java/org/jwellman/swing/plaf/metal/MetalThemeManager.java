package org.jwellman.swing.plaf.metal;

/* http://www.javapractices.com/topic/TopicAction.do?Id=158

 Copyright (c) 2002-2018 Hirondelle Systems.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of Hirondelle Systems nor the
       names of its contributors may be used to endorse or promote products
       derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY HIRONDELLE SYSTEMS ''AS IS'' AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL HIRONDELLE SYSTEMS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

import javax.swing.plaf.metal.*;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
* Defines all themes which can allow the user to customize the Java Look and Feel.
*
* <P>This application uses only the cross-platform Java Look-and-Feel, and never
* attempts to adapt to the native look-and-feel (Windows, Metal, Mac).
*
* @author Rick Wellman
*/
public final class MetalThemeManager {

  /*
  * Implementation Note:
  * This item has not been converted to an enum class, since it
  * contains so many data-related settings.
  */

  /**
  * A theme identical to the default Java look-and-feel, but whose name
  * is "Default" instead of the cryptic "Steel", and which provides a
  * <tt>toString</tt> method (required if <tt>Theme</tt> objects
  * passed to a combo box). Used as the base class for all other themes
  * used in this application.
  */
  public static final MetalTheme DEFAULT = new Default();

  /**
  * Much like {@link #DEFAULT}, but uses some blue-green colors.
  */
  public static final MetalTheme AQUA = new Aqua();

  /**
   * Not like any of the others... more, well... "modern"
   */
  public static final MetalTheme MODERN = new Modern();

  /**
   * Trying to emulate colors and design of https://www.deviantart.com/z-design/art/Z-DESIGN-Tech-Brush-Set-v6-287214074
   */
  public static final MetalTheme XP = new Experimental();
  
  /**
  * Differs from {@link #DEFAULT} only in font sizes.
  */
  public static final MetalTheme LARGE_FONT = new LargeFont();

  /**
  * Large fonts, and high contrast black and white colors.
  *
  * <P>This is an amalgam of two example themes from the JDK swing examples ; there
  * is apparently no recommended standard for a low-vision theme.
  */
  public static final MetalTheme LOW_VISION = new LowVision();

  /**
  * Convert <tt>aText</tt> into its corresponding <tt>Theme</tt> object,
  * if possible.
  *
  * @param aText possibly-null text which may map to a Theme.
  * @return null if <tt>aText</tt> is null, else try to match to a
  * known <tt>Theme</tt>.
  * @throws IllegalArgumentException if <tt>aText</tt> cannot be
  * matched to a known theme.
  */
  public static MetalTheme valueOf(String aText) {
    if (aText == null) return null;
    for(MetalTheme theme: VALUES){
      if (aText.endsWith(theme.getName())){
        return theme;
      }
    }
    throw new IllegalArgumentException("Cannot parse into Theme object:" + aText);
  }

  /**
  * Return true if <tt>aTheme</tt> uses a larger font than the default; this is the
  * case only for <tt>LARGE_FONT</tt> and <tt>LOW_VISION</tt>.
  *
  * <P>Themes with large font sizes need particular care, as their use may
  * require changes outside those provided through
  * <tt>SwingUtilities.updateComponentTreeUI</tt>.
  */
  public static boolean hasLargeFont(MetalTheme aTheme) {
    return aTheme == LARGE_FONT || aTheme == LOW_VISION;
  }

  private static final MetalTheme[] fValues = {
    DEFAULT,
    AQUA,
    MODERN,
    XP,
    LARGE_FONT,
    LOW_VISION
  };

  /**Allows user to iterate over all elements of this enumeration.  */
  public static final java.util.List<MetalTheme> VALUES =
    Collections.unmodifiableList(Arrays.asList(fValues)
  );

  private static final String fDEFAULT_NAME = "Default";
  private static final String fAQUA_NAME = "Aqua";
  private static final String fLARGE_FONT_NAME = "Large Font";
  private static final String fLOW_VISION_NAME = "Low Vision";
  private static final String fMODERN_NAME = "Modern";

    /**
     * 
     * @param aTheme
     * @param aFrame
     */
    public static void update(MetalTheme aTheme , JFrame aFrame) {
        MetalLookAndFeel.setCurrentTheme ( aTheme ) ;
        try {
            UIManager.setLookAndFeel ( new MetalLookAndFeel ( ) ) ;
        } catch ( UnsupportedLookAndFeelException ex ) {
            System.out.println ( "Cannot set new Theme for Java Look and Feel." ) ;
        }

        SwingUtilities.updateComponentTreeUI ( aFrame ) ;
    }

    /*
     * All items below are private nested classes which define the various themes.
     */
    private static class Default extends DefaultMetalTheme {
        private final String fName = fDEFAULT_NAME ;

        public String getName() {
            return fName ;
        }

        /**
         * This override is provided such that Theme objects can be directly passed to
         * JComboBox, instead of Strings. (This would not be necessary if getName had
         * been named toString instead).
         */
        @ Override
        public final String toString() {
            return getName ( ) ;
        }

    }

    /**
        nimrodlf.b=#474544
        nimrodlf.w=#BBBBBB

        nimrodlf.p1=#57889C
        nimrodlf.p2=#DEDEDE
        nimrodlf.p3=#BBBBBB

        nimrodlf.s1=#009999
        nimrodlf.s2=#DEDEDE
        nimrodlf.s3=#EFEFEF

        nimrodlf.menuOpacity=195
        nimrodlf.frameOpacity=180
        nimrodlf.font=Open Sans-PLAIN-14

   */
  private static class Modern extends Default {
    public String getName(){ return fName; }
    private final String fName = fMODERN_NAME;
    private final ColorUIResource fBlack = new ColorUIResource(0x474544);
    private final ColorUIResource fWhite = new ColorUIResource(0xBBBBBB);
    private final ColorUIResource fPrimary1 = new ColorUIResource(0x009999);
    private final ColorUIResource fPrimary2 = new ColorUIResource(0xDEDEDE);
    private final ColorUIResource fPrimary3 = new ColorUIResource(0xBBBBBB);
    private final ColorUIResource fSecondary1 = new ColorUIResource(0x57889C);
    private final ColorUIResource fSecondary2 = new ColorUIResource(0xDEDEDE);
    private final ColorUIResource fSecondary3 = new ColorUIResource(0xEFEFEF);

    protected ColorUIResource getBlack() { return fBlack; }
    protected ColorUIResource getWhite() { return fWhite; }

    protected ColorUIResource getPrimary1() { return fPrimary1; }
    protected ColorUIResource getPrimary2() { return fPrimary2; }
    protected ColorUIResource getPrimary3() { return fPrimary3; }

    protected ColorUIResource getSecondary1() { return fSecondary1; }
    protected ColorUIResource getSecondary2() { return fSecondary2; }
    protected ColorUIResource getSecondary3() { return fSecondary3; }
  }

  private static class Experimental extends Default {

      public String getName(){ return fName; }
      private final String fName = fMODERN_NAME;
      
      private final ColorUIResource fBlack = new ColorUIResource(0x6C8697); // 0x6C8697 is dark-grayish 
      private final ColorUIResource fWhite = new ColorUIResource(0xFFFFFF); // 0xEFF0F2 is light-gray // background for JList, JTree, JText***, etc.

      private final ColorUIResource fRed   = new ColorUIResource(0xFF0000); // temporary... for helping figure out which color goes where
      private final ColorUIResource fSkyBlue  = new ColorUIResource(0x10A7FF);

      private final ColorUIResource fPrimary1 = new ColorUIResource(0x6C8697); // 0x10A7FF is sky blue (0x009999); // Border of selected JInternalFrame
      private final ColorUIResource fPrimary2 = new ColorUIResource(0xDEDEDE); //(0xDEDEDE); // Background for... Scrollbar, JMenu rollover, ...
      private final ColorUIResource fPrimary3 = new ColorUIResource(0xBBBBBB); //(0xBBBBBB); // Background for... JFrame/JInternalFrame title bar, selected text/item 
      
      private final ColorUIResource fSecondary1 = new ColorUIResource(0x57889C); // getControlDarkShadow()
      //^^^ As its name implies, I'm pretty sure that the metal theme uses the "secondary1" color as a
      //^^^ complement to the "primary1" color.  i.e. Wherever you see primary1 used, it is likely/possible
      //^^^ that you will see secondary1 used as well.  Therefore, these two colors should "complement" each other.
      private final ColorUIResource fSecondary2 = new ColorUIResource(0xDEDEDE); // getControlShadow()
      private final ColorUIResource fSecondary3 = new ColorUIResource(0xEFF0F2); // 0xC5CED5 (0xEFEFEF); // getControl()
      // The name of this resource is a bit misleading IMO... it states that it is "secondary" and its 'importance' is "three"
      // However, ... as the comment states, this is actually the primary color of all "controls" which, as you might imagine,
      // would fairly dominate the typical user interface.  These are buttons, menus, separator bars, ...,
      // and last but not least, the background of a JPanel (which is not a control but DOES use this color).
      // 

      protected ColorUIResource getBlack() { return fBlack; }
      protected ColorUIResource getWhite() { return fWhite; }

      protected ColorUIResource getPrimary1() { return fPrimary1; }
      protected ColorUIResource getPrimary2() { return fPrimary2; }
      protected ColorUIResource getPrimary3() { return fPrimary3; }

      protected ColorUIResource getSecondary1() { return fSecondary1; }
      protected ColorUIResource getSecondary2() { return fSecondary2; }
      protected ColorUIResource getSecondary3() { return fSecondary3; }
      
      public ColorUIResource getWindowTitleForeground() { return fWhite; }
      public ColorUIResource getWindowTitleBackground() { return fSkyBlue; } // { return getPrimary3(); }
      public ColorUIResource getSeparatorForeground()   { return getBlack(); } // { return getPrimary1(); }
      public ColorUIResource getAcceleratorForeground() { return fRed; } // { return getPrimary1(); }
      
      
  }
  
  private static class Aqua extends Default {
    public String getName(){ return fName; }
    protected ColorUIResource getPrimary1() { return fPrimary1; }
    protected ColorUIResource getPrimary2() { return fPrimary2; }
    protected ColorUIResource getPrimary3() { return fPrimary3; }
    private final String fName = fAQUA_NAME;
    private final ColorUIResource fPrimary1 = new ColorUIResource(102, 153, 153);
    private final ColorUIResource fPrimary2 = new ColorUIResource(128, 192, 192);
    private final ColorUIResource fPrimary3 = new ColorUIResource(159, 235, 235);
  }

  private static class LargeFont extends Default {
    public String getName(){ return fName; }
    //fonts are larger than defaults
    public FontUIResource getControlTextFont() { return fControlFont;}
    public FontUIResource getSystemTextFont() { return fSystemFont;}
    public FontUIResource getUserTextFont() { return fUserFont;}
    public FontUIResource getMenuTextFont() { return fControlFont;}
    public FontUIResource getWindowTitleFont() { return fWindowTitleFont;}
    public FontUIResource getSubTextFont() { return fSmallFont;}
    private final String fName = fLARGE_FONT_NAME;
    private final FontUIResource fControlFont = new FontUIResource("Dialog", Font.BOLD, 18);
    private final FontUIResource fSystemFont = new FontUIResource("Dialog", Font.PLAIN, 18);
    private final FontUIResource fWindowTitleFont = new FontUIResource(
      "Dialog", Font.BOLD,18
    );
    private final FontUIResource fUserFont = new FontUIResource("SansSerif", Font.PLAIN, 18);
    private final FontUIResource fSmallFont = new FontUIResource("Dialog", Font.PLAIN, 14);
  }

  private static class LowVision extends LargeFont {
    public String getName() { return fName; }
    //colors are mostly black and white
    public ColorUIResource getPrimaryControlHighlight() { return fPrimaryHighlight;}
    public ColorUIResource getControlHighlight() { return super.getSecondary3(); }
    public ColorUIResource getFocusColor() { return getBlack(); }
    public ColorUIResource getTextHighlightColor() { return getBlack(); }
    public ColorUIResource getHighlightedTextColor() { return getWhite(); }
    public ColorUIResource getMenuSelectedBackground() { return getBlack(); }
    public ColorUIResource getMenuSelectedForeground() { return getWhite(); }
    public ColorUIResource getAcceleratorForeground() { return getBlack(); }
    public ColorUIResource getAcceleratorSelectedForeground() { return getWhite(); }
    public void addCustomEntriesToTable(UIDefaults aTable) {
      super.addCustomEntriesToTable(aTable);
      aTable.put( "ToolTip.border", fBlackLineBorder);
      aTable.put( "TitledBorder.border", fBlackLineBorder);
      aTable.put( "ScrollPane.border", fBlackLineBorder);
      aTable.put( "TextField.border", fTextBorder);
      aTable.put( "PasswordField.border", fTextBorder);
      aTable.put( "TextArea.border", fTextBorder);
      aTable.put( "TextPane.border", fTextBorder);
      aTable.put( "EditorPane.border", fTextBorder);
      aTable.put(
        "InternalFrame.closeIcon",
        MetalIconFactory.getInternalFrameCloseIcon(fInternalFrameIconSize)
      );
      aTable.put(
        "InternalFrame.maximizeIcon",
        MetalIconFactory.getInternalFrameMaximizeIcon(fInternalFrameIconSize)
      );
      aTable.put(
        "InternalFrame.iconifyIcon",
         MetalIconFactory.getInternalFrameMinimizeIcon(fInternalFrameIconSize)
       );
      aTable.put(
        "InternalFrame.minimizeIcon",
         MetalIconFactory.getInternalFrameAltMaximizeIcon(fInternalFrameIconSize)
       );
      aTable.put( "ScrollBar.width", fScrollBarWidth );
    }
    protected ColorUIResource getPrimary1() { return fPrimary1; }
    protected ColorUIResource getPrimary2() { return fPrimary2; }
    protected ColorUIResource getPrimary3() { return fPrimary3; }
    protected ColorUIResource getSecondary2() { return fSecondary2; }
    protected ColorUIResource getSecondary3() { return fSecondary3; }
    private static final String fName = fLOW_VISION_NAME;
    private final ColorUIResource fPrimary1 = new ColorUIResource(0, 0, 0);
    private final ColorUIResource fPrimary2 = new ColorUIResource(204, 204, 204);
    private final ColorUIResource fPrimary3 = new ColorUIResource(255, 255, 255);
    private final ColorUIResource fPrimaryHighlight = new ColorUIResource(102,102,102);
    private final ColorUIResource fSecondary2 = new ColorUIResource(204, 204, 204);
    private final ColorUIResource fSecondary3 = new ColorUIResource(255, 255, 255);
    private final Border fBlackLineBorder = new BorderUIResource(new LineBorder(getBlack()));
    private final Object fTextBorder = new BorderUIResource(
      new CompoundBorder(fBlackLineBorder, new BasicBorders.MarginBorder())
    );
    private final int fInternalFrameIconSize = 30;
    private final Integer fScrollBarWidth = new Integer(25);
  }
}