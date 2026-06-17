package org.jwellman.virtualdesktop.vapps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jwellman.demo.textdrag.DragDropCredentialPanel;

/**
 * Vapp that hosts the DragDropCredentialPanel, loaded from vapps-config.json.
 *
 * Expected attrs:
 *   salt        - shared salt used when encrypting values and pre-filled in the generator
 *   description - pipe-delimited list of labels for each draggable credential row
 *   value       - pipe-delimited list of salt-encrypted hashes, one per description
 *
 * @author rwellman
 */
public class SpecDragDropCredential extends VirtualAppSpec implements Configurable {

    public SpecDragDropCredential() {
        this.setTitle("Credentials");
    }

    @Override
    public void configure(Map<String, String> attrs) {
        String salt = attrs.getOrDefault("salt", "");
        List<String> descriptions = splitPipe(attrs.get("description"));
        List<String> values = splitPipe(attrs.get("value"));

        DragDropCredentialPanel panel = new DragDropCredentialPanel(salt, descriptions, values);
        this.setContent(this.createDefaultContent(panel));
    }

    private List<String> splitPipe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        return Arrays.asList(value.split("\\|"));
    }

}
