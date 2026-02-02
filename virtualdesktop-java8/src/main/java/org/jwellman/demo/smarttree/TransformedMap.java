package org.jwellman.demo.smarttree;

//Create a simple subclass so we can identify it in the Registry
public class TransformedMap extends java.util.ArrayList<PropertyPair> {
    public TransformedMap(int initialCapacity) {
        super(initialCapacity);
    }
}
