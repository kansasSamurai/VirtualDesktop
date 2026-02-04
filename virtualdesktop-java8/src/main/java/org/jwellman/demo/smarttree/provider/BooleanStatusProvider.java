package org.jwellman.demo.smarttree.provider;

import org.jwellman.demo.smarttree.NodeFormatProvider;
import org.jwellman.demo.smarttree.SmartTreePanel;
import org.jwellman.demo.smarttree.SummaryRegistry;

public class BooleanStatusProvider implements NodeFormatProvider {

    @Override
    public String format(SmartTreePanel.PropertyPair pair, SummaryRegistry summaries) {
        if (pair.getValue() instanceof Boolean) {
            boolean active = (Boolean) pair.getValue();
            String label = active ? "✔ ACTIVE" : "✘ INACTIVE";
            String color = active ? "#6a8759" : "#808080";

            return "<html><b>" + pair.getName() + "</b>: " +
                   "<font color='" + color + "'>[" + label + "]</font></html>";
        }
        return null;
    }

}
