package org.jwellman.demo.d3ish;

import java.util.ArrayList;
import java.util.List;

public class CircularIterator<E> {
    private final List<E> elements;
    private int index = 0;

    public CircularIterator(List<E> elements) {
        if (elements == null || elements.isEmpty()) {
            throw new IllegalArgumentException("List cannot be empty");
        }
        this.elements = new ArrayList<>(elements); // Defensive copy
    }

    public E next() {
        E item = elements.get(index);
        index = (index + 1) % elements.size();
        return item;
    }

}
