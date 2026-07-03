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
 * Self-describing diagram of the LayeredDiagramTool's framework layer:
 * the api interfaces and their core implementations.
 *
 * Layout:
 *   Row 1 (y=40):  5 api interfaces
 *   Row 2 (y=255): 5 core implementations (one under each interface)
 *   Row 3 (y=460): CanvasOverlayPanel (below EdgeRenderPanel)
 *
 * Each implementor shares the same center-x as its interface,
 * so the "implements" edges are perfectly straight vertical lines.
 */
public class ToolFrameworkDiagram {

    private ToolFrameworkDiagram() {}

    public static void buildDemo(DiagramLayeredPane pane, CanvasComponentFactory factory) {
        // --- api interfaces (Row 1) ---
        NodeHostPanel nCanvasTheme = makeNode("nf-ctheme", "INTERFACE", "CanvasTheme",
            null,
            Arrays.asList("+ getCanvasBackground(): Color",
                          "+ getNodeHeaderBackground(s): Color",
                          "+ getEdgeColor(): Color"),
            factory, pane, 40, 40, 220, 145);

        NodeHostPanel nGraphNode = makeNode("nf-gnode", "INTERFACE", "GraphNode",
            null,
            Arrays.asList("+ getNodeId(): String",
                          "+ getPortLocation(id): Point",
                          "+ getVisualComponent(): JComponent",
                          "+ invalidatePortCache()"),
            factory, pane, 300, 40, 225, 165);

        NodeHostPanel nGraphEdge = makeNode("nf-gedge", "INTERFACE", "GraphEdge",
            null,
            Arrays.asList("+ getEdgeId(): String",
                          "+ getSourceNodeId(): String",
                          "+ getAttributes(): EdgeAttributes"),
            factory, pane, 565, 40, 220, 145);

        NodeHostPanel nEdgeRouter = makeNode("nf-router", "INTERFACE", "EdgeRouter",
            null,
            Arrays.asList("+ calculatePath(...): Path2D",
                          "+ getApproachPoint(...): Point"),
            factory, pane, 825, 40, 220, 125);

        NodeHostPanel nCompFactory = makeNode("nf-cfactory", "INTERFACE", "CanvasComponentFactory",
            null,
            Arrays.asList("+ createContentFor(type, props): JPanel",
                          "+ getPortIds(type): String[]"),
            factory, pane, 1085, 40, 235, 125);

        // --- core implementations (Row 2) ---
        // Each node shares the same x and width as its interface above
        NodeHostPanel nBlueprint = makeNode("nf-blueprint", "CLASS", "BlueprintCanvasTheme",
            Arrays.asList("- PRUSSIAN_BLUE: Color", "- PALE_BLUE: Color"),
            null,
            factory, pane, 40, 255, 220, 120);

        NodeHostPanel nNodeHost = makeNode("nf-nodehost", "CLASS", "NodeHostPanel",
            Arrays.asList("- nodeId: String",
                          "- portCache: Map<String,Point>"),
            Arrays.asList("+ getPortLocation(id): Point",
                          "+ invalidatePortCache()"),
            factory, pane, 300, 255, 225, 160);

        NodeHostPanel nDefEdge = makeNode("nf-defedge", "CLASS", "DefaultGraphEdge",
            Arrays.asList("- sourceNodeId: String",
                          "- attributes: EdgeAttributes"),
            Arrays.asList("+ getEdgeId(): String",
                          "+ getAttributes(): EdgeAttributes"),
            factory, pane, 565, 255, 220, 160);

        NodeHostPanel nOrthoRtr = makeNode("nf-ortho", "CLASS", "OrthogonalRouter",
            null,
            Arrays.asList("+ calculatePath(...): Path2D",
                          "+ getApproachPoint(...): Point"),
            factory, pane, 825, 255, 220, 120);

        NodeHostPanel nEdgePanel = makeNode("nf-edgepanel", "CLASS", "EdgeRenderPanel",
            Arrays.asList("- edges: List<GraphEdge>",
                          "- nodeIndex: Map<String,GraphNode>"),
            Arrays.asList("+ addEdge(e: GraphEdge)",
                          "+ nodeUpdated(nodeId: String)"),
            factory, pane, 1085, 255, 235, 160);

        // --- core component without a direct interface (Row 3) ---
        NodeHostPanel nOverlay = makeNode("nf-overlay", "CLASS", "CanvasOverlayPanel",
            Arrays.asList("- state: State"),
            Arrays.asList("+ enterEdgeCreationMode()",
                          "+ exitEdgeCreationMode()"),
            factory, pane, 1085, 465, 235, 140);

        // --- implements edges (dashed + open arrowhead) ---
        // Each edge is a straight vertical line: same center-x, impl.N → interface.S
        EdgeAttributes implAttr = new EdgeAttributes();
        implAttr.setLineStyle(EdgeAttributes.LineStyle.DASHED);
        implAttr.setArrowType(EdgeAttributes.ArrowType.OPEN);

        pane.addGraphEdge(new DefaultGraphEdge("ef-bp-ct",  "nf-blueprint", "N", "nf-ctheme",   "S", implAttr));
        pane.addGraphEdge(new DefaultGraphEdge("ef-nh-gn",  "nf-nodehost",  "N", "nf-gnode",    "S", implAttr));
        pane.addGraphEdge(new DefaultGraphEdge("ef-de-ge",  "nf-defedge",   "N", "nf-gedge",    "S", implAttr));
        pane.addGraphEdge(new DefaultGraphEdge("ef-or-er",  "nf-ortho",     "N", "nf-router",   "S", implAttr));
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
