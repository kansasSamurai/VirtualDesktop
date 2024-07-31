package org.jwellman.applet.scar;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jwellman.swing.layout.WrapLayout;

public class FieldHolder extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    @SuppressWarnings("unused")
    private Scar mediator;
    private Map<String, String> fields; // = new HashMap(); // string, string
    private Map<String, Field> values; // = new HashMap(); // string, jtextfield

    private JPanel innerholder = new JPanel();
    
    public FieldHolder(Scar m) {
        super();
        this.setLayout(new BorderLayout());

        this.mediator = m;

        innerholder.setLayout(new WrapLayout()); // (new FluidLayout());
        this.add(innerholder, BorderLayout.CENTER);
    }

    @SuppressWarnings("rawtypes")
    private void print(Map s) { }
    private void print(String s) { }

    public Map<String, String> getTokenMap() {
        for (String key : this.values.keySet()) {
            String value = this.values.get(key).getTextfield().getText();

            // Convert to string literal via apostrophe (')
            // unless a period (.) indicates to use literal string
            if (value.startsWith(".")) {
                value = value.substring(1);
            } else {
                value = "'" + value + "'";
            }
            this.fields.put(key, value);
        }
        return this.fields;
    }

    /**
     * Accept map of tokens and create text fields for each.
     * (remove existing text fields if necessary)
     * 
     * @param tokens
     */
    public void acceptNewTokens(Map<String,String> tokens) {
        print("tokens: "); print(tokens);

                this.fields = tokens;
                this.values = new HashMap<String,Field>();
                this.innerholder.removeAll();

                for (String key : tokens.keySet()) {
                    JTextField tf = this.createField(key);
                    tf.setColumns(30);
                    tf.setFont(MONO);
                    Field wrapper = new Field(tf);
                    // weird bug(?): I cannot add JTextField objects to the map so I created the Field class (I **can** however add JPanel objects so I really don't know what's going on)
                    this.values.put(key, wrapper);
                    this.innerholder.add(wrapper);
                }

                this.innerholder.revalidate();

                print("fields: "); print(this.fields);
                print("values: "); print(this.values);

//        SwingUtilities.invokeLater(ui());

    }

    public JTextField createField(String label) {
        JTextField a = new JTextField("value-goes-here");
        a.setBorder(new javax.swing.border.TitledBorder(label));

        return a;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public Map<String, Field> getValues() {
        return values;
    }

    public void setValues(Map<String, Field> values) {
        this.values = values;
    }

}
