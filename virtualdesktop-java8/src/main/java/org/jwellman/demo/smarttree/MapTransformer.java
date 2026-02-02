package org.jwellman.demo.smarttree;

import java.util.Map;

import org.jwellman.demo.smarttree.SmartTreePanel.PropertyPair;

/**
 * Transforms java.util.Map for a better tree view.
 * 
 * @author rwellman
 *
 */
public class MapTransformer implements TransformationProvider {

    @Override
    public Object transform(Object obj) {
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            TransformedMap entries = new TransformedMap(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.add(new PropertyPair(String.valueOf(entry.getKey()), entry.getValue()));
            }
            return entries; 
        }
        return obj;
    }

}
