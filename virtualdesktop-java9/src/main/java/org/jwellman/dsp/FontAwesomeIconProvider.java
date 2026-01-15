package org.jwellman.dsp;

import java.awt.Color;

import javax.swing.Icon;

import org.jwellman.dsp.icons.IconProvider;
import org.jwellman.dsp.icons.IconSpecifier;

import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class FontAwesomeIconProvider implements IconProvider {
//  GoogleMaterialDesignIcons.BORDER_ALL, 48, Color.red)

    public void initialize() {
        IconFontSwing.register(FontAwesome.getIconFont());        
    }
    
    @Override
    public Icon getIcon(IconSpecifier specifier) {
        return IconFontSwing.buildIcon( FontAwesome.valueOf(specifier.getIconName()), 
            specifier.getSize(), specifier.getForeground());
    }

    @Override
    public Icon getIcon(IconSpecifier specifier, Color color) {
        return IconFontSwing.buildIcon( FontAwesome.valueOf(specifier.getIconName()), 
                specifier.getSize(), color);
    }

}
