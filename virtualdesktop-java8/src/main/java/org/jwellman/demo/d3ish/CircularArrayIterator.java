package org.jwellman.demo.d3ish;

public class CircularArrayIterator<E> {

    private final E[] elements;
    private int index = 0;

    public CircularArrayIterator(E[] elements) {
        if (elements == null || elements.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty");
        }
        // We store the reference to the array
        this.elements = elements;
    }

    /**
     * Thread-safe version to ensure "Forensic" logs or tasks 
     * don't collide when grabbing the next item.
     */
    public synchronized E next() {
        E item = elements[index];
        index = (index + 1) % elements.length;
        return item;
    }
    
    public int size() {
        return elements.length;
    }

}