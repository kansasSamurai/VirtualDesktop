package org.jwellman.diagram.domain.cls;

import java.util.Map;

import javax.swing.JPanel;

import org.jwellman.diagram.api.CanvasComponentFactory;

/**
 * Factory for class-diagram nodes. Supports node types "CLASS" and "INTERFACE".
 */
public class ClassDiagramFactory implements CanvasComponentFactory {

    @Override
    public JPanel createContentFor(String nodeType, Map<String, Object> properties) {
        String name       = (String) properties.getOrDefault("name", "Unnamed");
        Object fields     = properties.get("fields");
        Object methods    = properties.get("methods");
        return new ClassNodeContent(name, nodeType, fields, methods);
    }

    @Override
    public String[] getPortIds(String nodeType) {
        return new String[]{"N", "S", "E", "W"};
    }
}
