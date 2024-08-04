package org.jwellman.applet.scar;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * Search and Replace (ScAR)
 * <p>
 * A simple utility for searching for placeholders within templates
 * and replacing them with user defined values.  Original use case
 * was using this tool to convert MyBatis query template into actual
 * query with data.
 * <p>
 * Note:  This class and package are the result of the script regexv2.bsh
 * 
 * @author rwellman
 *
 */
public class Scar {

    private View view;
    private Result result;
    private Template template;
    private FieldHolder fieldHolder;
    private Grep btnGrep;
    private Parse btnParse;
    private Substitute btnSubstitute;
    private JTextField regex = new JTextField("(#\\{(.+?)\\})");
    private Options options;

    // delete me when finished; print() is from bsh
    private void print(String s) { }

    /**
     * What to do when user chooses 'parse'
     */
    public void parse() {
        print("pressed parse");

        String temp = this.template.getText();

        Map<String, String> tokens = this.getTokens(temp);

        this.fieldHolder.acceptNewTokens(tokens);

    }

    /**
     * What to do when user chooses 'substitute'
     * 
     * https://www.baeldung.com/java-regexp-escape-char
     * <([{\^-=$!|]})?*+.>
     * 
     */
    public void substitute() {

        String copy = template.getText();

        if (options.removeXML()) {
            copy = copy.replaceAll("\\<(.+?)\\>", "");
        }

        if (options.removeBlankLines()) {
            // trim result
            copy = copy.trim();

            // If removing XML leaves blank lines, remove them as well 
            copy = copy.replaceAll("(?m)^[ \\t]*\\r?\\n", "");
            //  BTW a shorter expression is: (?m)^\\s*\\n
//            copy
//            .replaceAll("(?m)^\\s+$", "")
//            .replaceAll("(?m)^\\n", "");
//            copy = copy.replaceAll("^\\s*\\n", "");
//            copy = copy.replaceAll("^\\s*\\r\\n", "");
//            copy = copy.replaceAll("^\\s*$", "<blank line>");
        }

        if (options.autoCopyResult()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(copy), null);
        }

        Map<String,String> tokens = this.fieldHolder.getTokenMap();
        for (String key : tokens.keySet()) {
            print("replace " + key + " with " + tokens.get(key));
            copy = copy.replace(key, tokens.get(key));
        }
        this.result.setText(copy);

    }

	public void grep() {
		// TODO Auto-generated method stub
		
	}

    private Map<String, String> getTokens(String input) {
        final Map<String, String> map = new HashMap<>();

        String patternString = this.regex.getText(); // "(\\$\\{(.+?)\\})";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            print("found: " + key + " " + value);
            map.put(key, value);
        }

        return map;
    }

	public void init() {
    }

    public JPanel createView() {
        this.view = new View(this);
        this.result = new Result(this);
        this.template = new Template(this);
        this.fieldHolder = new FieldHolder(this);
        this.btnGrep = new Grep(this);
        	this.btnGrep.addActionListener(this.view);
        this.btnParse = new Parse(this);
            this.btnParse.addActionListener(this.view);
        this.btnSubstitute = new Substitute(this);
            this.btnSubstitute.addActionListener(this.view);
        this.options = new Options();

        // CENTER (text areas)
        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        JScrollPane scroll = new JScrollPane();
        scroll.getViewport().add(template);
        sp.setTopComponent(scroll); //(this.template);

        scroll = new JScrollPane();
        scroll.getViewport().add(this.result);
        sp.setBottomComponent(scroll);
        sp.setDividerLocation(0.6);
        sp.setResizeWeight(0.6);

        JSplitPane mainsp = new JSplitPane();

        // EAST (field holder)
        JPanel east = new JPanel(new BorderLayout());

        // EAST top
        JPanel easttop = new JPanel();
        easttop.setLayout(new BoxLayout(easttop, BoxLayout.PAGE_AXIS));
        easttop.setBorder(new TitledBorder("Regular Expression"));
        east.add(easttop, BorderLayout.NORTH);

        // EAST top - regex
        easttop.add(regex);

        // EAST top - buttons
        JPanel panel = wrap(this.btnParse);
        panel.add(this.btnGrep);
        easttop.add(panel);

        // EAST fields
        scroll = new JScrollPane();
        scroll.setBorder(new TitledBorder("Fields"));
        scroll.getViewport().add(this.fieldHolder);
        east.add(scroll, BorderLayout.CENTER);

        // EAST bottom
        JPanel eastbottom = new JPanel();
        eastbottom.setLayout(new BoxLayout(eastbottom, BoxLayout.PAGE_AXIS));
        east.add(eastbottom, BorderLayout.SOUTH);

        eastbottom.add(wrap(this.btnSubstitute));

        panel = boxwrap(leftwrap(options.Chooser.REMOVE_XML));
        panel.add(leftwrap(options.Chooser.REMOVE_BLANKLINES));
        panel.add(leftwrap(options.Chooser.AUTO_COPY));
        panel.setBorder(new TitledBorder("Options"));
        eastbottom.add(panel);

        // final layout
        mainsp.setTopComponent(sp);
        mainsp.setBottomComponent(east);
        mainsp.setDividerLocation(0.75);
        mainsp.setResizeWeight(0.75);
        this.view.add(mainsp, BorderLayout.CENTER);

        // SOUTH (options and buttons)
//        panel = new JPanel(); // flow
////        panel.add(this.btnParse);
////        panel.add(this.btnSubstitute);
//        this.view.add(panel, BorderLayout.SOUTH);

        return this.view;
    }

    // create panel with flowlayout (center)
    protected JPanel wrap(JComponent c) {
        JPanel p = new JPanel();
        p.add(c);

        return p;
    }

    // create panel with flowlayout (left)
    protected JPanel leftwrap(JComponent c) {
        JPanel p = this.wrap(c);
        p.setLayout(new FlowLayout(FlowLayout.LEFT));

        return p;
    }

    protected JPanel boxwrap(JComponent c) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        p.add(c);

        return p;
    }

}
