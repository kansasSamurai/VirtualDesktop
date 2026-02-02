package org.jwellman.demo.smarttree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jwellman.demo.smarttree.SmartTreePanel.PropertyPair;

/**
 * 
 * @author rwellman
 *
 */
public class MapTransformer implements TransformationProvider {

    @Override
    public Object transform(Object obj) {
        if (obj instanceof Map) {
            // Convert Map to a List of custom Entry objects or PropertyPairs
            List<PropertyPair> entries = new ArrayList<>();
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.add(new PropertyPair(String.valueOf(entry.getKey()), entry.getValue()));
            }
            return entries;
        }
        return obj;
    }

}
