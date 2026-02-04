package org.jwellman.demo.smarttree.provider;

import org.jwellman.demo.smarttree.NodeFormatProvider;
import org.jwellman.demo.smarttree.SmartTreePanel;
import org.jwellman.demo.smarttree.SummaryRegistry;

public class ProgressBarProvider implements NodeFormatProvider {
    private final double max;
    private final String barColor;

    public ProgressBarProvider(double max, String barColor) {
        this.max = max;
        this.barColor = barColor;
    }

    @Override
    public String format(SmartTreePanel.PropertyPair pair, SummaryRegistry summaries) {
        Object val = pair.getValue();
        
        // Only apply to numbers that look like metrics (e.g., 'usage', 'load', 'percent')
        if (val instanceof Number && isMetricField(pair.getName())) {
            double current = ((Number) val).doubleValue();
            int percent = (int) Math.min(100, Math.max(0, (current / max) * 100));
            
            // Build the HTML Bar
            // We use a table to create a fixed-width container
            return String.format(
                "<html><b>%s</b>: <font color='#6a8759'>%.2f</font> " +
                "<table border='1' cellspacing='0' cellpadding='0' style='display:inline; margin-left:5px;'>" +
                "<tr><td width='50' height='10' bgcolor='#333333'>" +
                "<table border='0' cellspacing='0' cellpadding='0' width='%d%%'>" +
                "<tr><td height='8' bgcolor='%s'></td></tr>" +
                "</table>" +
                "</td></tr></table></html>",
                pair.getName(), current, percent, barColor
            );
        }
        return null;
    }

    private boolean isMetricField(String name) {
        String n = name.toLowerCase();
        return n.contains("usage") || n.contains("load") || n.contains("capacity") || n.contains("progress");
    }

}
