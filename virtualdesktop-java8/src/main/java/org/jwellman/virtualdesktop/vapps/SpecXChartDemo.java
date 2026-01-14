/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jwellman.virtualdesktop.vapps;

/**
 *
 * @author Rick
 */
public class SpecXChartDemo extends VirtualAppSpec {
    
    public SpecXChartDemo() {
        this.setTitle("XChart Demo");
        this.setContent(new com.xeiam.xchart.demo.XChartDemo());
    }
    
}
