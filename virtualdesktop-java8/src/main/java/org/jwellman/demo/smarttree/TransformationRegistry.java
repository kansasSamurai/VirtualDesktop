package org.jwellman.demo.smarttree;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author rwellman
 *
 */
public class TransformationRegistry {

    private final List<TransformationProvider> providers = new ArrayList<>();

    public void addProvider(TransformationProvider provider) {
        providers.add(provider);
    }

    public Object transform(Object obj) {
        Object current = obj;
        for (TransformationProvider provider : providers) {
            current = provider.transform(current);
        }
        return current;
    }

}
