package org.jwellman.diagram.domain.cls;

import java.util.Arrays;
import java.util.HashMap;
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
        NodeHostPanel nSpec = makeNode("n-spec", "CLASS", "SpecDiagramTool",
            null,
            Arrays.asList("+ SpecDiagramTool()"),
            factory, pane, 40, 40, 185, 115);

        NodeHostPanel nTool = makeNode("n-tool", "CLASS", "LayeredDiagramTool",
            Arrays.asList("- diagramPane: DiagramLayeredPane"),
            Arrays.asList("+ getDiagramPane()", "+ setComponentFactory(f)"),
            factory, pane, 285, 40, 240, 145);

        NodeHostPanel nPropEd = makeNode("n-proped", "CLASS", "PropertyEditorPanel",
            Arrays.asList("- selectedComponent: JComponent"),
            Arrays.asList("+ setSelectedComponent(c)", "+ clearSelection()"),
            factory, pane, 570, 40, 225, 145);

        // --- Row 2: canvas + sub-panels ---
        NodeHostPanel nCdf = makeNode("n-cdf", "CLASS", "ClassDiagramFactory",
            Arrays.asList("- theme: CanvasTheme"),
            Arrays.asList("+ createContentFor(type, props)", "+ getPortIds(type): String[]"),
            factory, pane, 40, 235, 215, 150);

        NodeHostPanel nPane = makeNode("n-pane", "CLASS", "DiagramLayeredPane",
            Arrays.asList("- graphNodes: Map<String,GraphNode>",
                          "- edgePanel: EdgeRenderPanel"),
            Arrays.asList("+ addGraphNode(n, layer)", "+ addGraphEdge(e)"),
            factory, pane, 285, 235, 240, 155);

        NodeHostPanel nColorProp = makeNode("n-colorprop", "CLASS", "ColorPropertyPanel",
            Arrays.asList("- swatches: ColorSwatch[]"),
            Arrays.asList("+ setColor(c: Color)"),
            factory, pane, 570, 235, 225, 130);

        // --- Row 3: domain and element types ---
        NodeHostPanel nContent = makeNode("n-content", "CLASS", "ClassNodeContent",
            Arrays.asList("- theme: CanvasTheme"),
            Arrays.asList("+ ClassNodeContent(name, type, ...)"),
            factory, pane, 40, 450, 215, 130);

        NodeHostPanel nShape = makeNode("n-shape", "CLASS", "DiagramShape",
            Arrays.asList("- shapeType: ShapeType", "- fillColor: Color"),
            Arrays.asList("+ paintComponent(g)", "+ setShapeType(t)"),
            factory, pane, 285, 450, 215, 155);

        NodeHostPanel nText = makeNode("n-text", "CLASS", "DiagramText",
            Arrays.asList("- cornerRadius: int"),
            Arrays.asList("+ setCornerRadius(r)", "+ getText(): String"),
            factory, pane, 570, 450, 215, 145);

        // --- edges ---
        // "creates" / "injects" = dashed + open arrowhead
        EdgeAttributes createAttr = new EdgeAttributes();
        createAttr.setLineStyle(EdgeAttributes.LineStyle.DASHED);
        createAttr.setArrowType(EdgeAttributes.ArrowType.OPEN);

        // "has-a" / "manages" = solid + filled arrowhead
        EdgeAttributes hasAttr = new EdgeAttributes();

        // SpecDiagramTool creates LayeredDiagramTool
        pane.addGraphEdge(new DefaultGraphEdge("et-spec-tool", "n-spec", "E", "n-tool", "W", createAttr));
        // SpecDiagramTool creates ClassDiagramFactory
        pane.addGraphEdge(new DefaultGraphEdge("et-spec-cdf", "n-spec", "S", "n-cdf", "N", createAttr));
        // LayeredDiagramTool has PropertyEditorPanel
        pane.addGraphEdge(new DefaultGraphEdge("et-tool-prop", "n-tool", "E", "n-proped", "W", hasAttr));
        // LayeredDiagramTool has DiagramLayeredPane
        pane.addGraphEdge(new DefaultGraphEdge("et-tool-pane", "n-tool", "S", "n-pane", "N", hasAttr));
        // PropertyEditorPanel has ColorPropertyPanel
        pane.addGraphEdge(new DefaultGraphEdge("et-prop-color", "n-proped", "S", "n-colorprop", "N", hasAttr));
        // ClassDiagramFactory creates ClassNodeContent
        pane.addGraphEdge(new DefaultGraphEdge("et-cdf-cnt", "n-cdf", "S", "n-content", "N", createAttr));
        // DiagramLayeredPane hosts DiagramShape
        pane.addGraphEdge(new DefaultGraphEdge("et-pane-shape", "n-pane", "S", "n-shape", "N", hasAttr));
        // DiagramLayeredPane hosts DiagramText (E→N L-path: right then down)
        pane.addGraphEdge(new DefaultGraphEdge("et-pane-text", "n-pane", "E", "n-text", "N", hasAttr));
    }

    private static NodeHostPanel makeNode(String id, String type, String name,
                                          java.util.List<String> fields,
                                          java.util.List<String> methods,
                                          CanvasComponentFactory factory,
                                          DiagramLayeredPane pane,
                                          int x, int y, int w, int h) {
        Map<String, Object> props = new HashMap<>();
        props.put("name", name);
        if (fields  != null) { props.put("fields",  fields); }
        if (methods != null) { props.put("methods", methods); }

        JPanel content = factory.createContentFor(type, props);
        NodeHostPanel node = new NodeHostPanel(id, type, props, content);
        node.setBounds(x, y, w, h);
        pane.addGraphNode(node, DiagramLayeredPane.SHAPE_LAYER);
        return node;
    }
}
