package org.jwellman.applet.scar;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

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

        Map<String,String> tokens = this.fieldHolder.getTokenMap();
        for (String key : tokens.keySet()) {
            print("replace " + key + " with " + tokens.get(key));
            copy = copy.replace(key, tokens.get(key));
        }
        this.result.setText(copy);

    }

    public void init() {
    }

    public JPanel createView() {
        this.view = new View(this);
        this.result = new Result(this);
        this.template = new Template(this);
        this.fieldHolder = new FieldHolder(this);
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
        sp.setDividerLocation(0.5);

        JSplitPane mainsp = new JSplitPane();
        mainsp.setTopComponent(sp);

        scroll = new JScrollPane();
        scroll.getViewport().add(this.fieldHolder);
        mainsp.setBottomComponent(scroll);
        mainsp.setDividerLocation(0.5);
        this.view.add(mainsp, BorderLayout.CENTER);

        // EAST (field holder)
        // moved to splitpane
        // this.view.add(fieldHolder, BorderLayout.EAST);

        // SOUTH (options and buttons)
        JPanel panel = new JPanel(); // flow
        panel.add(new JLabel("RegExp:"));
        panel.add(regex );
        panel.add(this.btnParse);
        panel.add(this.btnSubstitute);
        panel.add(options.Chooser.REMOVE_XML);
        this.view.add(panel, BorderLayout.SOUTH);

        return this.view;
    }

}

