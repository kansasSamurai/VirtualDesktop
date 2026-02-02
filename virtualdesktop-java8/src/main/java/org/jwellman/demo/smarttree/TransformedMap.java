package org.jwellman.demo.smarttree;

import org.jwellman.demo.smarttree.SmartTreePanel.PropertyPair;

//Create a simple subclass so we can identify it in the Registry
@SuppressWarnings("serial")
public class TransformedMap extends java.util.ArrayList<PropertyPair> {

    public TransformedMap(int initialCapacity) {
        super(initialCapacity);
    }

}
