package org.jwellman.virtualdesktop.vapps;

import org.jfree.chart.demo.JFreeChartDemo;

/**
 *
 * @author Rick Wellman
 */
public class SpecJFreeChart extends VirtualAppSpec {

    public SpecJFreeChart() {
        super();
        this.setTitle("JFreeChart");
        
        JFreeChartDemo app = JFreeChartDemo.createMainUI(null);
        this.setContent( app.getPanel() );
    }

}
