package com.docs_similarity.dsa;

/**
 * Linear time consistency checks that run after every analysis, so the "verified" status shown
 * in the dashboard is the result of a real check instead of a hard coded tick.
 *
 * <ol>
 *   <li>The Suffix Array is a permutation of 0 .. n-1 (every suffix appears exactly once).</li>
 *   <li>For every pair of neighbours SA[r-1], SA[r] with h = lcp[r]: their last common symbol
 *       really is equal, and at offset h the earlier suffix either ends or has a strictly
 *       smaller symbol. This confirms the sorted order and that h is where they first differ.</li>
 * </ol>
 * Both checks together take O(n).
 */
public final class SuffixArrayVerifier {

    private SuffixArrayVerifier() {
    }

    public static boolean verify(int[] text, int[] suffixArray, int[] lcp) {
        return isPermutation(suffixArray, text.length) && isOrderedByLcp(text, suffixArray, lcp);
    }

    static boolean isPermutation(int[] suffixArray, int n) {
        if (suffixArray.length != n) {
            return false;
        }
        boolean[] seen = new boolean[n];
        for (int position : suffixArray) {
            if (position < 0 || position >= n || seen[position]) {
                return false;
            }
            seen[position] = true;
        }
        return true;
    }

    static boolean isOrderedByLcp(int[] text, int[] suffixArray, int[] lcp) {
        final int n = text.length;
        for (int r = 1; r < n; r++) {
            int a = suffixArray[r - 1];
            int b = suffixArray[r];
            int h = lcp[r];
            if (h < 0 || a + h > n || b + h > n) {
                return false;
            }
            if (h > 0 && text[a + h - 1] != text[b + h - 1]) {
                return false;                   // claimed common prefix is not common
            }
            if (a + h == n) {
                continue;                       // a is a prefix of b, so a correctly comes first
            }
            if (b + h == n || text[a + h] >= text[b + h]) {
                return false;                   // wrong order, or the prefix is actually longer
            }
        }
        return true;
    }
}
