package org.jwellman.virtualdesktop.vapps;

import java.util.Properties;

import org.jdatepicker.impl.DateComponentFormatter;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

/**
 * A demonstration of the JDatePicker component
 * 
 * Online References:
 * https://github.com/JDatePicker/JDatePicker
 * https://www.codejava.net/java-se/swing/how-to-use-jdatepicker-to-display-calendar-component
 * https://stackoverflow.com/questions/26794698/how-do-i-implement-jdatepicker
 * https://www.programcreek.com/java-api-examples/?api=org.jdatepicker.impl.JDatePanelImpl
 * https://www.programcreek.com/java-api-examples/index.php?api=org.jdatepicker.impl.JDatePickerImpl
 * 
 * @author rwellman
 *
 */
public class SpecJDatePicker extends VirtualAppSpec {

	UtilDateModel model = new UtilDateModel();
	JDatePanelImpl datePanel;
	JDatePickerImpl datePicker;
	
	static Properties p;
	
	static {
		p = new Properties();
		p.put("text.today", "today");
		p.put("text.month", "month");
		p.put("text.year", "year");		
	}
	
	public SpecJDatePicker() {
		super();
//		this.setHeight(200);
//		this.setWidth(200);
		this.setTitle("JDatePicker Demo");
		
		datePanel = new JDatePanelImpl(model, p);
		datePicker = new JDatePickerImpl(datePanel, new DateComponentFormatter() );
		
        this.setContent(this.createDefaultContent(datePicker));
	}
	
}
