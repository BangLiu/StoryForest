package edu.ualberta.storyteller.core.util;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * This class contains methods that manipulating arrays.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class ArrayUtils {

    /**
     * Get K largest value in an array.
     * <p>
     * @param array Input array of type T.
     * @param k We want to get k-th largest value from array. It start from 0.
     * @return k-th largest value in array. k >= 0.
     */
    public static <T extends Comparable<T>> T largestK(T[] array, int k) {
        if (k <= 0) {
            System.out.println("k must be >= 0.");
            return null;
        }
        PriorityQueue<T> queue = new PriorityQueue<>(k+1);
        int i = 0;
        while (i<=k) {
            queue.add(array[i]);
            i++;
        }
        for (; i < array.length; i++) {
            T value = queue.peek();
            if (array[i].compareTo(value) > 0) {
                queue.poll();
                queue.add(array[i]);
            }
        }
        return queue.peek();
    }

    /**
     * Calc length of Longest Increasing Subsequence (LIS).
     * <p>
     * @param nums Input array.
     * @return Length of LIS.
     */
    public static int lengthOfLIS(int[] nums) {
        int[] dp = new int[nums.length];
        int len = 0;
        for (int num : nums) {
            int i = Arrays.binarySearch(dp, 0, len, num);
            if (i < 0) {
                i = -(i + 1);
            }
            dp[i] = num;
            if (i == len) {
                len++;
            }
        }
        return len;
    }
}
