package org.jwellman.dsp;

import java.awt.Color;

import javax.swing.Icon;

import org.jwellman.dsp.icons.IconProvider;
import org.jwellman.dsp.icons.IconSpecifier;

import jiconfont.icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;

public class GoogleMaterialIconProvider implements IconProvider {

    public void initialize() {
        IconFontSwing.register(GoogleMaterialDesignIcons.getIconFont());
    }

    @Override
    public Icon getIcon(IconSpecifier specifier) {
        return IconFontSwing.buildIcon(GoogleMaterialDesignIcons.valueOf(specifier.getIconName()),
            specifier.getSize(), specifier.getForeground());
    }

    @Override
    public Icon getIcon(IconSpecifier specifier, Color color) {
        return IconFontSwing.buildIcon(GoogleMaterialDesignIcons.valueOf(specifier.getIconName()),
            specifier.getSize(), color);
    }

}
