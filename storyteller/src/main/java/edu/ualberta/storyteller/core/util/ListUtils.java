package edu.ualberta.storyteller.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains methods that manipulating lists.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class ListUtils {

    /**
     * Find the most common element in a list.
     * <p>
     * @param list Input list.
     * @param <T> List type.
     * @return The most common element.
     */
    public static <T> T mostCommon(List<T> list) {
        Map<T, Integer> map = new HashMap<>();
        for (T t : list) {
            Integer val = map.get(t);
            map.put(t, val == null ? 1 : val + 1);
        }
        Map.Entry<T, Integer> max = null;
        for (Map.Entry<T, Integer> e : map.entrySet()) {
            if (max == null || e.getValue() > max.getValue()) {
                max = e;
            }
        }
        return max.getKey();
    }

}
