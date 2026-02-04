package org.jwellman.demo.smarttree.provider;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jwellman.demo.smarttree.NodeFormatProvider;
import org.jwellman.demo.smarttree.SmartTreePanel;
import org.jwellman.demo.smarttree.SummaryRegistry;

/**
 * 
 * @author rwellman
 *
 */
public class NumericAlertProvider implements NodeFormatProvider {

    private String normalColor = "#6a8759"; // Default Green
    private String alertColor = "#ff6b68"; // Alert Red
    private String warningColor = "#ffc66d"; // Warning Orange

    private BigDecimal minNormal;
    private BigDecimal maxNormal;

    private BigDecimal convert(Number value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        } else {
            // valueOf(double) handles double/float/int/long safely
            // and is generally preferred over 'new BigDecimal(double)'
            return BigDecimal.valueOf(value.doubleValue());
        }
    }
    
    @Override
    public String format(SmartTreePanel.PropertyPair pair, SummaryRegistry summaries) {
        Object val = pair.getValue();
        if (val instanceof Number) {
            String color = getNormalColor();

            BigDecimal num = convert((Number) val);
            if ( num.compareTo(getMinNormal()) < 0 )
                color = getAlertColor();
            else if ( num.compareTo(maxNormal) > 0 )
                color = getWarningColor();

            return "<html><b>" + pair.getName() + "</b>: " + "<font color='" + color + "'>" + num + "</font></html>";
        }
        return null;
    }

    public String getNormalColor() {
        return normalColor;
    }

    public void setNormalColor(String normalColor) {
        this.normalColor = normalColor;
    }

    public String getAlertColor() {
        return alertColor;
    }

    public void setAlertColor(String alertColor) {
        this.alertColor = alertColor;
    }

    public String getWarningColor() {
        return warningColor;
    }

    public void setWarningColor(String warningColor) {
        this.warningColor = warningColor;
    }

    protected BigDecimal getMinNormal() {
        return minNormal;
    }

    public void setMinNormal(Number minNormal) {
        this.minNormal = convert(minNormal);
    }

    protected BigDecimal getMaxNormal() {
        return maxNormal;
    }

    public void setMaxNormal(Number maxNormal) {
        this.maxNormal = convert(maxNormal);
    }

}
