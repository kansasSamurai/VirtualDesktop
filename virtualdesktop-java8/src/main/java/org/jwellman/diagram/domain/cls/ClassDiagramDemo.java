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
 * Populates a DiagramLayeredPane with an illustrative class relationship diagram.
 * Demonstrates that real JPanel nodes and typed edges coexist on the canvas.
 */
public class ClassDiagramDemo {

    private ClassDiagramDemo() {}

    public static void buildDemo(DiagramLayeredPane pane, CanvasComponentFactory factory) {
        // --- nodes ---
        NodeHostPanel iRepo     = makeNode("n-irepo",     "INTERFACE", "IRepository",
            null,
            Arrays.asList("+ findById(id): T", "+ save(entity: T): void"),
            factory, pane, 80, 80, 200, 130);

        NodeHostPanel user      = makeNode("n-user",      "CLASS", "User",
            Arrays.asList("- id: Long", "- name: String", "- email: String"),
            Arrays.asList("+ getId(): Long", "+ getName(): String"),
            factory, pane, 380, 80, 210, 160);

        NodeHostPanel userRepo  = makeNode("n-userrepo",  "CLASS", "UserRepository",
            Arrays.asList("- dataSource: DataSource"),
            Arrays.asList("+ findById(id): User", "+ save(u: User): void"),
            factory, pane, 80, 320, 220, 160);

        NodeHostPanel userSvc   = makeNode("n-usersvc",   "CLASS", "UserService",
            Arrays.asList("- repo: UserRepository"),
            Arrays.asList("+ findUser(id): User", "+ saveUser(u: User): void"),
            factory, pane, 380, 320, 210, 160);

        // --- edges ---
        // UserRepository implements IRepository (dashed, open arrow)
        EdgeAttributes implAttr = new EdgeAttributes();
        implAttr.setLineStyle(EdgeAttributes.LineStyle.DASHED);
        implAttr.setArrowType(EdgeAttributes.ArrowType.OPEN);
        pane.addGraphEdge(new DefaultGraphEdge(
            "e-impl", "n-userrepo", "N", "n-irepo", "S", implAttr));

        // UserService depends on UserRepository (solid, filled arrow)
        EdgeAttributes depAttr = new EdgeAttributes();
        pane.addGraphEdge(new DefaultGraphEdge(
            "e-dep", "n-usersvc", "W", "n-userrepo", "E", depAttr));

        // UserService uses User (solid, filled arrow)
        EdgeAttributes useAttr = new EdgeAttributes();
        pane.addGraphEdge(new DefaultGraphEdge(
            "e-use", "n-usersvc", "N", "n-user", "S", useAttr));
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
