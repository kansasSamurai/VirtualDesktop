package org.jwellman.jfreechart.editor;

import java.awt.Color;
import java.awt.Font;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

public class SubtitleEditor extends TitleEditor {

    private Title subtitle;

    private static final long serialVersionUID = 1L;

    public SubtitleEditor() {
        super();
    }

    @Override
    public void updateChart() {
        if (subtitle instanceof TextTitle) {
            TextTitle t = (TextTitle)subtitle;
            t.setText(txtTitle.getText());

            Font c = t.getFont();
            String name = StringUtils.isEmpty(txtTitleFont.getText())
                ?  c.getFontName() : txtTitleFont.getText() ;
            Font f = new Font(name, c.getStyle(), fontsize.getValue());
            t.setFont(f);

            Color pc = pnlPaletteChooser.getSelectedColor();
            if (pc != null)
                t.setPaint(pc);
        }
        if (subtitle instanceof LegendTitle) {
            LegendTitle t = (LegendTitle)subtitle;

            Font c = t.getItemFont();
            String name = StringUtils.isEmpty(txtTitleFont.getText())
                ?  c.getFontName() : txtTitleFont.getText() ;
            Font f = new Font(name, c.getStyle(), fontsize.getValue());
            t.setItemFont(f);

            Color pc = pnlPaletteChooser.getSelectedColor();
            if (pc != null) t.setItemPaint(pc);
        }

        statusLabel.setText(STATUS.SAVED);
        this.showSavedMessage();
        statusLabel.setText(STATUS.READY);
    }

    public Title getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(Title subtitle) {
        this.subtitle = subtitle;
    }

}
