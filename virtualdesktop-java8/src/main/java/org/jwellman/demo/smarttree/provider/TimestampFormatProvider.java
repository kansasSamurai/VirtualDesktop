package org.jwellman.demo.smarttree.provider;

import org.jwellman.demo.smarttree.NodeFormatProvider;
import org.jwellman.demo.smarttree.SmartTreePanel;
import org.jwellman.demo.smarttree.SummaryRegistry;

public class TimestampFormatProvider implements NodeFormatProvider {

    private final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(SmartTreePanel.PropertyPair pair, SummaryRegistry summaries) {
        // Intercept if the field name suggests a time or the value is a Date
        if (pair.getName().toLowerCase().contains("time") || pair.getValue() instanceof java.util.Date) {
            Object val = pair.getValue();
            String dateStr = (val instanceof java.util.Date) 
                             ? sdf.format((java.util.Date)val) 
                             : "<i>(Not a Date object)</i>";

            return "<html><font color='#9876aa'><b>" + pair.getName() + "</b></font>: " +
                   "<font color='#4FC3F7'>📅 " + dateStr + "</font></html>";
        }
        return null;
    }

}
