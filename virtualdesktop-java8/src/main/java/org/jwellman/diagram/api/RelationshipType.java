package org.jwellman.diagram.api;

import java.util.Arrays;
import java.util.List;

/**
 * Named preset of EdgeAttributes representing a UML relationship type.
 * {@link #defaultUmlTypes()} returns the six standard class-diagram relationships.
 */
public class RelationshipType {

    public final String name;
    public final EdgeAttributes attributes;

    public RelationshipType(String name, EdgeAttributes attributes) {
        this.name       = name;
        this.attributes = new EdgeAttributes(attributes);
    }

    public static List<RelationshipType> defaultUmlTypes() {
        return Arrays.asList(
            association(),
            inheritance(),
            realization(),
            dependency(),
            aggregation(),
            composition()
        );
    }

    private static RelationshipType association() {
        EdgeAttributes a = new EdgeAttributes();
        a.setLineStyle(EdgeAttributes.LineStyle.SOLID);
        a.setArrowType(EdgeAttributes.ArrowType.NONE);
        a.setSourceArrowType(EdgeAttributes.ArrowType.NONE);
        return new RelationshipType("Association", a);
    }

    private static RelationshipType inheritance() {
        EdgeAttributes a = new EdgeAttributes();
        a.setLineStyle(EdgeAttributes.LineStyle.SOLID);
        a.setArrowType(EdgeAttributes.ArrowType.OPEN);
        a.setSourceArrowType(EdgeAttributes.ArrowType.NONE);
        return new RelationshipType("Inheritance", a);
    }

    private static RelationshipType realization() {
        EdgeAttributes a = new EdgeAttributes();
        a.setLineStyle(EdgeAttributes.LineStyle.DASHED);
        a.setArrowType(EdgeAttributes.ArrowType.OPEN);
        a.setSourceArrowType(EdgeAttributes.ArrowType.NONE);
        return new RelationshipType("Realization", a);
    }

    private static RelationshipType dependency() {
        EdgeAttributes a = new EdgeAttributes();
        a.setLineStyle(EdgeAttributes.LineStyle.DASHED);
        a.setArrowType(EdgeAttributes.ArrowType.FILLED);
        a.setSourceArrowType(EdgeAttributes.ArrowType.NONE);
        return new RelationshipType("Dependency", a);
    }

    private static RelationshipType aggregation() {
        EdgeAttributes a = new EdgeAttributes();
        a.setLineStyle(EdgeAttributes.LineStyle.SOLID);
        a.setArrowType(EdgeAttributes.ArrowType.NONE);
        a.setSourceArrowType(EdgeAttributes.ArrowType.OPEN_DIAMOND);
        return new RelationshipType("Aggregation", a);
    }

    private static RelationshipType composition() {
        EdgeAttributes a = new EdgeAttributes();
        a.setLineStyle(EdgeAttributes.LineStyle.SOLID);
        a.setArrowType(EdgeAttributes.ArrowType.NONE);
        a.setSourceArrowType(EdgeAttributes.ArrowType.FILLED_DIAMOND);
        return new RelationshipType("Composition", a);
    }
}
