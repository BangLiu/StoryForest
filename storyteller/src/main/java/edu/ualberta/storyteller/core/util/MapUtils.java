package edu.ualberta.storyteller.core.util;

import java.util.HashMap;

/**
 * This class contains methods that manipulating maps.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class MapUtils {

    /**
     * Calculate the number of intersect elements between two maps.
     * <p>
     * @param dc1 One hash map.
     * @param dc2 Another hash map.
     * @return Number of common elements.
     */
    public static int numIntersect(HashMap dc1, HashMap dc2) {
        int intersect = 0;
        for (Object key : dc1.keySet()) {
            if (dc2.containsKey(key)) {
                intersect++;
            }
        }
        return intersect;
    }

}
