package org.jwellman.virtualdesktop.vapps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jwellman.demo.textdrag.DragDropSnippetPanel;

/**
 * Vapp that hosts the DragDropSnippetPanel, loaded from vapps-config.json.
 *
 * Expected attrs:
 *   seed        - shared seed used when encoding values and pre-filled in the transform utility
 *   description - pipe-delimited list of labels for each draggable snippet row
 *   value       - pipe-delimited list of encoded values, one per description
 *
 * @author rwellman
 */
public class SpecDragDropSnippet extends VirtualAppSpec implements Configurable {

    public SpecDragDropSnippet() {
        this.setTitle("Snippets");
    }

    @Override
    public void configure(Map<String, String> attrs) {
        String seed = attrs.getOrDefault("seed", "");
        List<String> descriptions = splitPipe(attrs.get("description"));
        List<String> values = splitPipe(attrs.get("value"));

        DragDropSnippetPanel panel = new DragDropSnippetPanel(seed, descriptions, values);
        this.setContent(this.createDefaultContent(panel));
    }

    private List<String> splitPipe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        return Arrays.asList(value.split("\\|"));
    }

}
