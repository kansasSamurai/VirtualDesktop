package org.jwellman.jcalendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.toedter.calendar.IDateEvaluator;
import com.toedter.calendar.MinMaxDateEvaluator;

/**
 * A modified version of JDayChooser bean for choosing a day.
 * -todo- Make "day labels" optional
 * -todo- Create new background color for "days"; it currently only uses the LAF (unless selected)
 * -todo- Create new foreground color for "today"; it is currently shared with the "sundayForeground" property
 * -todo- Create new background color for "today"; it is currently shared with the "dayBackgroundColor" property
 * 
 * @author rwellman
 * 
 */
public class VDayChooser extends JPanel implements ActionListener, KeyListener, FocusListener {

    private static final long serialVersionUID = 5876398337018781820L;

    private static final int SUNDAY = 1;
    
    // Buttons representing each day of the month; note that indexes 0-6 actually refer to "dayNames" column headers
    protected JButton[] days;

    // Buttons for the week numbers; optionally displayed on the left
    protected JButton[] weeks;

    // Button reference to keep track of the selected day; note that whatever button it is, it is also in the "days" array
    protected JButton selectedDay;

    // A visual container for the "weeks" array buttons
    protected JPanel weekPanel;

    // A visual container for the "days" array buttons
    protected JPanel dayPanel;

    // The currently selected day of the month (1-31); 0 when nothing selected
    protected int day;

    // The current month being displayed
    protected int month;
    
	// The current year being displayed
    protected int year;
    
    protected Color oldDayBackgroundColor;

    protected Color selectedColor; // newf; accessors - this was previously not exposed

    protected Color sundayForeground;

    protected Color weekdayForeground;

    protected Color decorationBackgroundColor;
    
    // The background color for the "days" array buttons; can be overridden via the corresponding property.
    protected Color dayBackgroundColor = Color.white; // newf

    // A default Insets object for the "days" array buttons; can be overridden via the corresponding property.
    protected Insets dayMargin = new Insets(0, 0, 0, 0);

    // A data container for the "dayNames" column headers
    protected String[] dayNames;

    // A Calendar object for the entire month
    protected Calendar calendar;

    // A Calendar object only representing "today"
    protected Calendar today;

    // A Locale object for the entire day chooser
    protected Locale locale;

    protected boolean initialized;

    protected boolean weekOfYearVisible;

    protected boolean decorationBackgroundVisible = false;

    protected boolean decorationBordersVisible;

    protected boolean dayHeadersVisible = true; // newf
    
	protected boolean dayBordersVisible;

    private boolean alwaysFireDayProperty;

    // The maximum number of characters in the day header string; enforced to be 0-4 -- zero means use default shortdays based on locale
    protected int maxDayCharacters;

    protected List<IDateEvaluator> dateEvaluators;
    
    protected MinMaxDateEvaluator minMaxDateEvaluator;
    
    /**
     * Default JDayChooser constructor.
     */
    public VDayChooser() {
        this(false);
    }

    /**
     * JDayChooser constructor.
     * 
     * @param weekOfYearVisible
     *            true, if the weeks of a year shall be shown
     */
    public VDayChooser(boolean weekOfYearVisible) {
        setName("JDayChooser");
        setBackground(Color.blue);

        dateEvaluators = new ArrayList<>(1);
        minMaxDateEvaluator = new MinMaxDateEvaluator();
        addDateEvaluator(minMaxDateEvaluator);

        this.weekOfYearVisible = weekOfYearVisible;
        locale = Locale.getDefault();
        days = new JButton[49];
        selectedDay = null;
        calendar = Calendar.getInstance(locale);
        today = (Calendar) calendar.clone();

        setLayout(new BorderLayout());

        dayPanel = new JPanel();
        dayPanel.setLayout(new GridLayout(7, 7));

        sundayForeground = new Color(164, 0, 0); // = redish
        weekdayForeground = new Color(0, 90, 164); // = blueish

        // decorationBackgroundColor = new Color(194, 211, 252);
        // decorationBackgroundColor = new Color(206, 219, 246);
        decorationBackgroundColor = new Color(210, 228, 238); // = light-blueish

        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                int index = x + (7 * y);

                if (y == 0) {
                    // Create a button that doesn't react on clicks or focus changes.
                    // Thanks to Thomas Schaefer for the focus hint :)
                    days[index] = new DecoratorButton();
                	days[index].setVisible(dayHeadersVisible);
                } else {
                    days[index] = new CustomButton();  
                    	// new JButton("x") {
//                        private static final long serialVersionUID = -7433645992591669725L;
//
//                        public void paint(Graphics g) {
//                            if ("Windows".equals(UIManager.getLookAndFeel()
//                                    .getID())) {
//                                // this is a hack to get the background painted
//                                // when using Windows Look & Feel
//                                if (selectedDay == this) {
//                                    g.setColor(selectedColor);
//                                    g.fillRect(0, 0, getWidth(), getHeight());
//                                }
//                            }
//                            super.paint(g);
//                        }
//
//                    };
                    days[index].addActionListener(this);
                    days[index].addKeyListener(this);
                    days[index].addFocusListener(this);
                }

                // Apply this code to both 0-6 (headers) and 7-41 (days)
                days[index].setMargin(dayMargin);
                days[index].setFocusPainted(false);
                dayPanel.add(days[index]);
                	
            }
        }

        // Always build the weekPanel; however, whether it is shown is controlled by this.weekOfYearVisible
        weekPanel = new JPanel();
        weekPanel.setLayout(new GridLayout(7, 1));

        weeks = new JButton[7];
        for (int i = 0; i < 7; i++) {
            weeks[i] = new DecoratorButton();
            weeks[i].setMargin(dayMargin); // re-using dayMargin for weeks... TBD if we need/want a separate property
            weeks[i].setFocusPainted(false);
            weeks[i].setForeground(new Color(100, 100, 100));

            if (i != 0) {
                weeks[i].setText("0" + (i + 1)); // this default text seems pointless... TBD
            }

            weekPanel.add(weeks[i]);
        }

        init();

        setDay(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        add(dayPanel, BorderLayout.CENTER);

        if (weekOfYearVisible) {
            add(weekPanel, BorderLayout.WEST);
        }

        initialized = true;
        updateUI();
    }

    /**
     * Initializes the locale specific names for the days of the week.
     */
    protected void init() {
        JButton testButton = new CustomButton(); // JButton();
        oldDayBackgroundColor = testButton.getBackground();
        selectedColor = new Color(160, 160, 160);

        Date date = calendar.getTime();
        calendar = Calendar.getInstance(locale);
        calendar.setTime(date);

        drawDayNames();
        drawDays();
    }

    /**
     * Draws the day names of the day columns.
     */
    private void drawDayNames() {
        if (dayHeadersVisible) {
        	
	    	// Initialize dayNames with full names based on the locale
	        final DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(locale);
	        dayNames = dateFormatSymbols.getShortWeekdays();
	
	        // Initialize day pointer before entering loop
	        int day = calendar.getFirstDayOfWeek();
	        for (int i = 0; i < 7; i++) {
	        	
	        	// Adjust dayName string length
	            if (maxDayCharacters > 0 && maxDayCharacters < 5) {
	                if (dayNames[day].length() >= maxDayCharacters) {
	                    dayNames[day] = dayNames[day].substring(0, maxDayCharacters);
	                }
	            }
	
	            // Update button properties
	            System.out.println("drawDayNames(): setting day button index " + i + " to " + day);
	            days[i].setText(dayNames[day]);
	            if (day == SUNDAY) {
	                days[i].setForeground(sundayForeground);
	            } else {
	                days[i].setForeground(weekdayForeground);
	            }
	
	            // Update day pointer (with rollover)
	            if (day < 7) {
	                day++;
	            } else {
	                day -= 6;
	            }
	            
	        } // end for
	        
        } // end if
    }

    /**
     * Initializes both day names and weeks of the year.
     */
    protected void initDecorations() {
        for (int x = 0; x < 7; x++) {
            days[x].setContentAreaFilled(decorationBackgroundVisible);
            days[x].setBorderPainted(decorationBordersVisible);
            days[x].invalidate();
            days[x].repaint();
            weeks[x].setContentAreaFilled(decorationBackgroundVisible);
            weeks[x].setBorderPainted(decorationBordersVisible);
            weeks[x].invalidate();
            weeks[x].repaint();
        }
    }

    /**
     * Hides and shows the week buttons.
     */
    protected void drawWeeks() {
    	
        final Calendar tmpCalendar = (Calendar) calendar.clone();
        for (int i = 1; i < 7; i++) {
        	// Use the temporary calendar for calculations
            tmpCalendar.set(Calendar.DAY_OF_MONTH, (i * 7) - 6);
            int week = tmpCalendar.get(Calendar.WEEK_OF_YEAR);
            
            // Set the text on the week button
            String buttonText = Integer.toString(week);
            if (week < 10) {
                buttonText = "0" + buttonText;
            }
            weeks[i].setText(buttonText);

            // Rows 5 and 6 do not always have a day in the month (i.e. Feb. only has 28 days), 
            // so only make visible if there are days in this row
            if ((i == 5) || (i == 6)) {
                weeks[i].setVisible(days[i * 7].isVisible());
            }
        }
    }

    /**
     * Hides and shows the day buttons.
     */
    protected void drawDays() {
        final Calendar tmpCalendar = (Calendar) calendar.clone();
        tmpCalendar.set(Calendar.HOUR_OF_DAY, 0);
        tmpCalendar.set(Calendar.MINUTE, 0);
        tmpCalendar.set(Calendar.SECOND, 0);
        tmpCalendar.set(Calendar.MILLISECOND, 0);

        // initialize firstDayOfWeek before setting DAY_OF_MONTH 
        // int firstDayOfWeek = tmpCalendar.getFirstDayOfWeek();
        tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDay = tmpCalendar.get(Calendar.DAY_OF_WEEK) - tmpCalendar.getFirstDayOfWeek();
        if (firstDay < 0) {
            firstDay += 7;
        }

        int i;

        // Buttons in the 7x7 grid up until the first actual day are "turned off"
        for (i = 0; i < firstDay; i++) {
            days[i + 7].setVisible(false);
            days[i + 7].setText("");
        }

        // Get the first day in next month
        tmpCalendar.add(Calendar.MONTH, 1);
        final Date firstDayInNextMonth = tmpCalendar.getTime();
        tmpCalendar.add(Calendar.MONTH, -1);

        // Initialize vars used in loop
        Color foregroundColor = getForeground();
        Date day = tmpCalendar.getTime();

        int n = 0;
        while (day.before(firstDayInNextMonth)) {
        	
        	// TODO create a ref variable for days[i+n+7] for maintenance/readability
            days[i + n + 7].setText(Integer.toString(n + 1));
            days[i + n + 7].setVisible(true);

            if ((tmpCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
                    && (tmpCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR))) {
            	// If the day is "today", use a different foreground(text) color
            	// TODO This design seems limiting; it is using the sundayForeground as the "current day" foreground
                days[i + n + 7].setForeground(sundayForeground);
            } else {
            	// Otherwise, use the same foreground(text) color as the look and feel
                days[i + n + 7].setForeground(foregroundColor);
            }

            // If drawing the currently selected day, set the background color appropriately
            if ((n + 1) == this.day) {
                days[i + n + 7].setBackground(selectedColor);
                selectedDay = days[i + n + 7];
            } else { // else, this day is NOT the currently selected day so set the background color appropriately
                days[i + n + 7].setBackground(oldDayBackgroundColor);
            }

            // Set all "active" days to enabled
            days[i + n + 7].setEnabled(true);

            // Decorate the current day according to the IDateEvaluator(s) on this day chooser 
            final Iterator<IDateEvaluator> iterator = dateEvaluators.iterator(); 
            while (iterator.hasNext()) {
                final IDateEvaluator dateEvaluator = (IDateEvaluator) iterator.next();
                if (dateEvaluator.isSpecial(day)) {
                    days[i + n + 7].setForeground(dateEvaluator.getSpecialForegroundColor());
                    days[i + n + 7].setBackground(dateEvaluator.getSpecialBackroundColor());
                    days[i + n + 7].setToolTipText(dateEvaluator.getSpecialTooltip());
                    days[i + n + 7].setEnabled(true);
                } 
                if (dateEvaluator.isInvalid(day)){
                    days[i + n + 7].setForeground(dateEvaluator.getInvalidForegroundColor());
                    days[i + n + 7].setBackground(dateEvaluator.getInvalidBackroundColor());
                    days[i + n + 7].setToolTipText(dateEvaluator.getInvalidTooltip());
                    days[i + n + 7].setEnabled(false);
                }
            }

            // Increment loop control vars
            n++;
            tmpCalendar.add(Calendar.DATE, 1);
            day = tmpCalendar.getTime();
        }

        // The remaining buttons in the 7x7 grid are "turned off"
        for (int k = n + i + 7; k < 49; k++) {
            days[k].setVisible(false);
            days[k].setText("");
        }

        drawWeeks();
    }

    /**
     * Returns the locale.
     * 
     * @return the locale value
     * 
     * @see #setLocale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale.
     * 
     * @param locale
     *            the new locale value
     * 
     * @see #getLocale
     */
    public void setLocale(Locale locale) {
        if (!initialized) {
            super.setLocale(locale);
        } else {
            this.locale = locale;
            super.setLocale(locale);
            init();
        }
    }

    /**
     * Sets the day. This is a bound property.
     * 
     * @param d
     *            the day
     * 
     * @see #getDay
     */
    public void setDay(int d) {
    	// This seems like it would hide logic errors on the client side; should we or should we throw an exception.
        if (d < 1) {
            d = 1;
        }
        
        final Calendar tmpCalendar = (Calendar) calendar.clone();
        tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);
        
        tmpCalendar.add(Calendar.MONTH, 1);
        tmpCalendar.add(Calendar.DATE, -1);
        int maxDaysInMonth = tmpCalendar.get(Calendar.DATE);
        if (d > maxDaysInMonth) {
            d = maxDaysInMonth;
        }

        int oldDay = day;
        day = d;

        // If there is a current/previous selected day, visually "unselect" it by updating its background color
        if (selectedDay != null) {
            selectedDay.setBackground(oldDayBackgroundColor);
            selectedDay.repaint();
        }

        // Now find the new selectedDay button; save it and update its background color to selected color
        // TODO maybe create a new internal property that points to the "first" day cell so we do not have to "search" for the cell?
        for (int i = 7; i < 49; i++) {
            if (days[i].getText().equals(Integer.toString(day))) {
                selectedDay = days[i];
                selectedDay.setBackground(selectedColor);
                break;
            }
        }

        if (alwaysFireDayProperty) {
            firePropertyChange("day", 0, day);
        } else {
            firePropertyChange("day", oldDay, day);
        }
    }

    /**
     * This is needed for JDateChooser.
     * 
     * @param alwaysFire
     *            true, if day property shall be fired every time a day is chosen.
     */
    public void setAlwaysFireDayProperty(boolean alwaysFire) {
        alwaysFireDayProperty = alwaysFire;
    }

    /**
     * Returns the selected day.
     * 
     * @return the day value
     * 
     * @see #setDay
     */
    public int getDay() {
        return day;
    }

    /**
     * Sets a specific month. 
     * This is needed for correct graphical representation of the days.
     * 
     * @param month
     *            the new month
     */
    public void setMonth(int month) {
    	this.month = month;
    	
        calendar.set(Calendar.MONTH, month);
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        if (day > maxDays) {
            day = maxDays;
        }

        drawDays();
    }

    /**
	 * @return the month
	 */
	public int getMonth() {
		return month;
	}

    /**
     * Sets a specific year. 
     * This is needed for correct graphical representation of the days.
     * 
     * @param year
     *            the new year
     */
    public void setYear(int year) {
    	this.year = year;
    	
        calendar.set(Calendar.YEAR, year);
        drawDays();
    }

	/**
	 * @return the year
	 */
	public int getYear() {
		return year;
	}

    /**
     * Sets a specific calendar. 
     * This is needed for correct graphical representation of the days.
     * 
     * @param calendar
     *            the new calendar
     */
    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
        drawDays();
    }

    /**
     * Sets the font property.
     * 
     * @param font
     *            the new font
     */
    public void setFont(Font font) {
        if (days != null) {
            for (int i = 0; i < 49; i++) {
                days[i].setFont(font);
            }
        }
        if (weeks != null) {
            for (int i = 0; i < 7; i++) {
                weeks[i].setFont(font);
            }
        }
    }

    /**
     * Sets the foregroundColor color.
     * 
     * @param foreground
     *            the new foregroundColor
     */
    public void setForeground(Color foreground) {
        super.setForeground(foreground);

        if (days != null) {
            for (int i = 7; i < 49; i++) {
                days[i].setForeground(foreground);
            }

            drawDays();
        }
    }

    /**
     * JDayChooser is the ActionListener for all day buttons.
     * 
     * @param e
     *            the ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton) e.getSource();
        String buttonText = button.getText();
        int day = new Integer(buttonText).intValue();
        setDay(day);
    }

    /**
     * JDayChooser is the FocusListener for all day buttons. 
     * (Added by Thomas Schaefer)
     * 
     * @param e
     *            the FocusEvent
     */
    /*
     * Code below commented out by Mark Brown on 24 Aug 2004. This code breaks
     * the JDateChooser code by triggering the actionPerformed method on the
     * next day button. This causes the date chosen to always be incremented by
     * one day.
     */
    public void focusGained(FocusEvent e) {
        // JButton button = (JButton) e.getSource();
        // String buttonText = button.getText();
        //
        // if ((buttonText != null) && !buttonText.equals("") &&
        // !e.isTemporary()) {
        // actionPerformed(new ActionEvent(e.getSource(), 0, null));
        // }
    }

    /**
     * Does nothing.
     * 
     * @param e
     *            the FocusEvent
     */
    public void focusLost(FocusEvent e) {
    }

    /**
     * JDayChooser is the KeyListener for all day buttons. 
     * (Added by Thomas Schaefer and modified by Austin Moore)
     * 
     * @param e
     *            the KeyEvent
     */
    public void keyPressed(KeyEvent e) {
        int offset =       (e.getKeyCode() == KeyEvent.VK_UP) 
        		? (-7) : ( (e.getKeyCode() == KeyEvent.VK_DOWN) 
        		? (+7) : ( (e.getKeyCode() == KeyEvent.VK_LEFT) 
        		? (-1) : ( (e.getKeyCode() == KeyEvent.VK_RIGHT) 
        		? (+1) : 
        			0    )));

        int newDay = getDay() + offset;

        if (   (newDay >= 1)
            && (newDay <= calendar.getMaximum(Calendar.DAY_OF_MONTH))) {
            setDay(newDay);
        }
    }

    /**
     * Does nothing.
     * 
     * @param e
     *            the KeyEvent
     */
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Does nothing.
     * 
     * @param e
     *            the KeyEvent
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Enable or disable the JDayChooser.
     * 
     * @param enabled
     *            The new enabled value
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        for (short i = 0; i < days.length; i++) {
            if (days[i] != null) {
                days[i].setEnabled(enabled);
            }
        }

        for (short i = 0; i < weeks.length; i++) {
            if (weeks[i] != null) {
                weeks[i].setEnabled(enabled);
            }
        }
    }

    /**
     * In some Countries it is often usefull to know in which week of the year a
     * date is.
     * 
     * @return boolean true, if the weeks of the year is shown
     */
    public boolean isWeekOfYearVisible() {
        return weekOfYearVisible;
    }

    /**
     * In some Countries it is often usefull to know in which week of the year a
     * date is.
     * 
     * @param weekOfYearVisible
     *            true, if the weeks of the year shall be shown
     */
    public void setWeekOfYearVisible(boolean weekOfYearVisible) {
        if (weekOfYearVisible == this.weekOfYearVisible) {
            return;
        }
        
        this.weekOfYearVisible = weekOfYearVisible;
        if (weekOfYearVisible) {
            add(weekPanel, BorderLayout.WEST);
        } else {
            remove(weekPanel);
        }

        validate();
        dayPanel.validate();
    }

    /**
     * Returns the day panel.
     * 
     * @return the day panel
     */
    public JPanel getDayPanel() {
        return dayPanel;
    }

    /**
     * Returns the color of the decoration (day names and weeks).
     * 
     * @return the color of the decoration (day names and weeks).
     */
    public Color getDecorationBackgroundColor() {
        return decorationBackgroundColor;
    }

    /**
     * Sets the background of days and weeks of year buttons.
     * 
     * @param decorationBackgroundColor
     *            The background to set
     */
    public void setDecorationBackgroundColor(Color decorationBackgroundColor) {
        this.decorationBackgroundColor = decorationBackgroundColor;

        if (days != null) {
            for (int i = 0; i < 7; i++) {
                days[i].setBackground(decorationBackgroundColor);
            }
        }

        if (weeks != null) {
            for (int i = 0; i < 7; i++) {
                weeks[i].setBackground(decorationBackgroundColor);
            }
        }
    }

    /**
     * Returns the Sunday foreground.
     * 
     * @return Color the Sunday foreground.
     */
    public Color getSundayForeground() {
        return sundayForeground;
    }

    /**
     * Returns the weekday foreground.
     * 
     * @return Color the weekday foreground.
     */
    public Color getWeekdayForeground() {
        return weekdayForeground;
    }

    /**
     * Sets the Sunday foreground.
     * 
     * @param sundayForeground
     *            The sundayForeground to set
     */
    public void setSundayForeground(Color sundayForeground) {
        this.sundayForeground = sundayForeground;
        drawDayNames();
        drawDays();
    }

    /**
     * Sets the weekday foreground.
     * 
     * @param weekdayForeground
     *            The weekdayForeground to set
     */
    public void setWeekdayForeground(Color weekdayForeground) {
        this.weekdayForeground = weekdayForeground;
        drawDayNames();
        drawDays();
    }

    /**
     * Requests that the selected day also have the focus.
     */
    public void setFocus() {
        if (selectedDay != null) {
            this.selectedDay.requestFocus();
        }
    }

    /**
     * The decoration background is the background color of the day titles and
     * the weeks of the year.
     * 
     * @return Returns true, if the decoration background is painted.
     */
    public boolean isDecorationBackgroundVisible() {
        return decorationBackgroundVisible;
    }

    /**
     * The decoration background is the background color of the day titles and
     * the weeks of the year.
     * 
     * @param decorationBackgroundVisible
     *            true, if the decoration background shall be painted.
     */
    public void setDecorationBackgroundVisible(
            boolean decorationBackgroundVisible) {
        this.decorationBackgroundVisible = decorationBackgroundVisible;
        initDecorations();
    }

    /**
     * The decoration border is the button border of the day titles and the
     * weeks of the year.
     * 
     * @return Returns true, if the decoration border is painted.
     */
    public boolean isDecorationBordersVisible() {
        return decorationBordersVisible;
    }

    public boolean isDayBordersVisible() {
        return dayBordersVisible;
    }

    public void removeDateEvaluator(IDateEvaluator dateEvaluator) {
        dateEvaluators.remove(dateEvaluator);
    }

	public Color getSelectedColor() {
		return selectedColor;
	}

	// TODO redraw/repaint since a visual attribute has changed
	public void setSelectedColor(Color selectedColor) {
		this.selectedColor = selectedColor;
		if (selectedDay != null)
			selectedDay.setBackground(this.selectedColor); // TODO test that this works without calling repaint()
	}

	public Color getDayBackgroundColor() {
		return dayBackgroundColor;
	}

	// TODO redraw/repaint since a visual attribute has changed
	public void setDayBackgroundColor(Color dayBackgroundColor) {
		this.dayBackgroundColor = dayBackgroundColor;
	}

    public boolean isDayHeadersVisible() {
		return dayHeadersVisible;
	}

	public void setDayHeadersVisible(boolean dayHeadersVisible) {
		this.dayHeadersVisible = dayHeadersVisible;
	}

    /**
     * The decoration border is the button border of the day titles and the
     * weeks of the year.
     * 
     * @param decorationBordersVisible
     *            true, if the decoration border shall be painted.
     */
    public void setDecorationBordersVisible(boolean decorationBordersVisible) {
        this.decorationBordersVisible = decorationBordersVisible;
        initDecorations();
    }

    public void setDayBordersVisible(boolean dayBordersVisible) {
        this.dayBordersVisible = dayBordersVisible;
        if (initialized) {
            for (int x = 7; x < 49; x++) {
                if ("Windows".equals(UIManager.getLookAndFeel().getID())) {
                	System.out.print(".");
                    days[x].setContentAreaFilled(dayBordersVisible);
                } else {
                    // days[x].setContentAreaFilled(true);
                }
                days[x].setBorderPainted(dayBordersVisible);
            }
            System.out.println("X");
        }
    }

    /**
     * Updates the UI and sets the day button preferences.
     */
    public void updateUI() {
        super.updateUI();
        setFont(Font.decode("Dialog Plain 11"));

        if (weekPanel != null) {
            weekPanel.updateUI();
        }
        if (initialized) {
            if ("Windows".equals(UIManager.getLookAndFeel().getID())) {
                setDayBordersVisible(false);
                setDecorationBackgroundVisible(true);
                setDecorationBordersVisible(false);
            } else {
                setDayBordersVisible(true);
                setDecorationBackgroundVisible(decorationBackgroundVisible);
                setDecorationBordersVisible(decorationBordersVisible);
            }
        }
    }

    /**
     * Sets a valid date range for selectable dates. If max is before min, the
     * default range with no limitation is set.
     * 
     * @param min
     *            the minimum selectable date or null (then the minimum date is
     *            set to 01\01\0001)
     * @param max
     *            the maximum selectable date or null (then the maximum date is
     *            set to 01\01\9999)
     */
    public void setSelectableDateRange(Date min, Date max) {
        minMaxDateEvaluator.setMaxSelectableDate(max);
        minMaxDateEvaluator.setMinSelectableDate(min);
        drawDays();
    }

    /**
     * Sets the maximum selectable date. If null, the date 01\01\9999 will be
     * set instead.
     * 
     * @param max
     *            the maximum selectable date
     * 
     * @return the maximum selectable date
     */
    public Date setMaxSelectableDate(Date max) {
        Date maxSelectableDate = minMaxDateEvaluator.setMaxSelectableDate(max);
        drawDays();
        return maxSelectableDate;
    }

    /**
     * Sets the minimum selectable date. If null, the date 01\01\0001 will be
     * set instead.
     * 
     * @param min
     *            the minimum selectable date
     * 
     * @return the minimum selectable date
     */
    public Date setMinSelectableDate(Date min) {
        Date minSelectableDate = minMaxDateEvaluator.setMinSelectableDate(min);
        drawDays();
        return minSelectableDate;
    }

    /**
     * Gets the maximum selectable date.
     * 
     * @return the maximum selectable date
     */
    public Date getMaxSelectableDate() {
        return minMaxDateEvaluator.getMaxSelectableDate();
    }

    /**
     * Gets the minimum selectable date.
     * 
     * @return the minimum selectable date
     */
    public Date getMinSelectableDate() {
        return minMaxDateEvaluator.getMinSelectableDate();
    }

    /**
     * Gets the maximum number of characters of a day name or 0. If 0 is
     * returned, dateFormatSymbols.getShortWeekdays() will be used.
     * 
     * @return the maximum number of characters of a day name or 0.
     */
    public int getMaxDayCharacters() {
        return maxDayCharacters;
    }

    /**
     * Sets the maximum number of characters per day in the day bar. Valid
     * values are 0-4. If set to 0, dateFormatSymbols.getShortWeekdays() will be
     * used, otherwise theses strings will be reduced to the maximum number of
     * characters.
     * 
     * @param maxDayCharacters
     *            the maximum number of characters of a day name.
     */
    public void setMaxDayCharacters(int maxDayCharacters) {
        if (maxDayCharacters == this.maxDayCharacters) {
            return;
        }

        if (maxDayCharacters < 0 || maxDayCharacters > 4) {
            this.maxDayCharacters = 0;
        } else {
            this.maxDayCharacters = maxDayCharacters;
        }
        drawDayNames();
        drawDays();
        invalidate();
    }

    class CustomButton extends JButton {
        private static final long serialVersionUID = -5306477668406547495L;
        
        public CustomButton() {
            setBackground(getDayBackgroundColor());
            setContentAreaFilled(true);
            setBorderPainted(false); // true works        	
        }

        public void setBackground(Color c) {
        	System.out.println(c);
        	super.setBackground(c); // (Color.white);
        }
        
        public boolean isFocusable() {
            return true;
        }

    }
    
    class DecoratorButton extends JButton {
        private static final long serialVersionUID = -5306477668406547496L;

        public DecoratorButton() {
            setBackground(decorationBackgroundColor);
            setContentAreaFilled(decorationBackgroundVisible);
            setBorderPainted(decorationBordersVisible);
        }

        public void addMouseListener(MouseListener l) {
        }

        public boolean isFocusable() {
            return false;
        }

        public void paint(Graphics g) {
            if ("Windows".equals(UIManager.getLookAndFeel().getID())) {
                // this is a hack to get the background painted
                // when using Windows Look & Feel
                if (decorationBackgroundVisible) {
                    g.setColor(decorationBackgroundColor);
                } else {
                    g.setColor(days[7].getBackground());
                }
                g.fillRect(0, 0, getWidth(), getHeight());
                if (isBorderPainted()) {
                    setContentAreaFilled(true);
                } else {
                    setContentAreaFilled(false);
                }
            }
            super.paint(g);
        }
    };

    public void addDateEvaluator(IDateEvaluator dateEvaluator) {
        dateEvaluators.add(dateEvaluator);
    }

}
