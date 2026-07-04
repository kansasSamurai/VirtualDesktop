package org.jwellman.diagram.domain.cls;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;

import org.jwellman.diagram.DiagramLayeredPane;
import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.core.DefaultGraphEdge;
import org.jwellman.diagram.core.NodeHostPanel;

/**
 * Self-describing diagram of the LayeredDiagramTool's UI and domain layer:
 * the tool panels, canvas components, and the class-diagram demo domain.
 *
 * Layout (3 columns × 3 rows):
 *   Col1(x=40)  Col2(x=285) Col3(x=570)
 *   Row1(y=40)  Spec        LayeredDiag  PropertyEditor
 *   Row2(y=235) ClassDiagF  DiagPane     ColorProp
 *   Row3(y=450) ClassNode   DiagShape    DiagText
 */
public class ToolDiagramDemo {

    private ToolDiagramDemo() {}

    public static void buildDemo(DiagramLayeredPane pane, CanvasComponentFactory factory) {
        // --- Row 1: entry point and top-level UI ---
        makeNode("n-spec", "CLASS", "SpecDiagramTool",
            null,
            Arrays.asList(makeMethod("+", "", "SpecDiagramTool()")),
            factory, pane, 40, 40, 185, 115);

        makeNode("n-tool", "CLASS", "LayeredDiagramTool",
            Arrays.asList(makeField("-", "DiagramLayeredPane", "diagramPane")),
            Arrays.asList(makeMethod("+", "DiagramLayeredPane", "getDiagramPane()"),
                          makeMethod("+", "void", "setComponentFactory(f)")),
            factory, pane, 285, 40, 240, 145);

        makeNode("n-proped", "CLASS", "PropertyEditorPanel",
            Arrays.asList(makeField("-", "JComponent", "selectedComponent")),
            Arrays.asList(makeMethod("+", "void", "setSelectedComponent(c)"),
                          makeMethod("+", "void", "clearSelection()")),
            factory, pane, 570, 40, 225, 145);

        // --- Row 2: canvas + sub-panels ---
        makeNode("n-cdf", "CLASS", "ClassDiagramFactory",
            Arrays.asList(makeField("-", "CanvasTheme", "theme")),
            Arrays.asList(makeMethod("+", "JPanel", "createContentFor(type, props)"),
                          makeMethod("+", "String[]", "getPortIds(type)")),
            factory, pane, 40, 235, 215, 150);

        makeNode("n-pane", "CLASS", "DiagramLayeredPane",
            Arrays.asList(makeField("-", "Map<String,GraphNode>", "graphNodes"),
                          makeField("-", "EdgeRenderPanel", "edgePanel")),
            Arrays.asList(makeMethod("+", "void", "addGraphNode(n, layer)"),
                          makeMethod("+", "void", "addGraphEdge(e)")),
            factory, pane, 285, 235, 240, 155);

        makeNode("n-colorprop", "CLASS", "ColorPropertyPanel",
            Arrays.asList(makeField("-", "ColorSwatch[]", "swatches")),
            Arrays.asList(makeMethod("+", "void", "setColor(c)")),
            factory, pane, 570, 235, 225, 130);

        // --- Row 3: domain and element types ---
        makeNode("n-content", "CLASS", "ClassNodeContent",
            Arrays.asList(makeField("-", "CanvasTheme", "theme")),
            Arrays.asList(makeMethod("+", "", "ClassNodeContent(name, type, ...)")),
            factory, pane, 40, 450, 215, 130);

        makeNode("n-shape", "CLASS", "DiagramShape",
            Arrays.asList(makeField("-", "ShapeType", "shapeType"),
                          makeField("-", "Color", "fillColor")),
            Arrays.asList(makeMethod("+", "void", "paintComponent(g)"),
                          makeMethod("+", "void", "setShapeType(t)")),
            factory, pane, 285, 450, 215, 155);

        makeNode("n-text", "CLASS", "DiagramText",
            Arrays.asList(makeField("-", "int", "cornerRadius")),
            Arrays.asList(makeMethod("+", "void", "setCornerRadius(r)"),
                          makeMethod("+", "String", "getText()")),
            factory, pane, 570, 450, 215, 145);

        // --- edges ---
        EdgeAttributes createAttr = new EdgeAttributes();
        createAttr.setLineStyle(EdgeAttributes.LineStyle.DASHED);
        createAttr.setArrowType(EdgeAttributes.ArrowType.OPEN);

        EdgeAttributes hasAttr = new EdgeAttributes();

        pane.addGraphEdge(new DefaultGraphEdge("et-spec-tool",   "n-spec",     "E", "n-tool",     "W", createAttr));
        pane.addGraphEdge(new DefaultGraphEdge("et-spec-cdf",    "n-spec",     "S", "n-cdf",      "N", createAttr));
        pane.addGraphEdge(new DefaultGraphEdge("et-tool-prop",   "n-tool",     "E", "n-proped",   "W", hasAttr));
        pane.addGraphEdge(new DefaultGraphEdge("et-tool-pane",   "n-tool",     "S", "n-pane",     "N", hasAttr));
        pane.addGraphEdge(new DefaultGraphEdge("et-prop-color",  "n-proped",   "S", "n-colorprop","N", hasAttr));
        pane.addGraphEdge(new DefaultGraphEdge("et-cdf-cnt",     "n-cdf",      "S", "n-content",  "N", createAttr));
        pane.addGraphEdge(new DefaultGraphEdge("et-pane-shape",  "n-pane",     "S", "n-shape",    "N", hasAttr));
        pane.addGraphEdge(new DefaultGraphEdge("et-pane-text",   "n-pane",     "E", "n-text",     "N", hasAttr));
    }

    private static Map<String, String> makeField(String vis, String type, String name) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("visibility", vis);
        m.put("type",       type);
        m.put("name",       name);
        return m;
    }

    private static Map<String, String> makeMethod(String vis, String type, String name) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("visibility", vis);
        m.put("type",       type);
        m.put("name",       name);
        return m;
    }

    private static NodeHostPanel makeNode(String id, String type, String name,
                                          java.util.List<?> fields,
                                          java.util.List<?> methods,
                                          CanvasComponentFactory factory,
                                          DiagramLayeredPane pane,
                                          int x, int y, int w, int h) {
        Map<String, Object> props = new java.util.HashMap<>();
        props.put("name", name);
        if (fields  != null) { props.put("fields",  fields); }
        if (methods != null) { props.put("methods", methods); }

        Runnable onModified = () -> pane.notifyModified();
        JPanel content = factory.createContentFor(type, props, onModified);
        NodeHostPanel node = new NodeHostPanel(id, type, props, content);
        node.setBounds(x, y, w, h);
        pane.addGraphNode(node, DiagramLayeredPane.SHAPE_LAYER);
        return node;
    }
}
