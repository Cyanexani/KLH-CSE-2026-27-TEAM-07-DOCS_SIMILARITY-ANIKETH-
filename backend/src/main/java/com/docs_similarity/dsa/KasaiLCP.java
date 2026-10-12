package com.docs_similarity.dsa;

/**
 * Builds the <b>LCP Array</b> from a Suffix Array using the <b>Kasai algorithm</b>
 * (Kasai, Lee, Arimura, Arikawa and Park, 2001).
 *
 * <h2>What is the LCP Array?</h2>
 * LCP stands for Longest Common Prefix. In this project:
 * <pre>
 *   lcp[0] = 0                                      (the first suffix has no predecessor)
 *   lcp[r] = length of the longest common prefix of the suffixes SA[r-1] and SA[r]
 * </pre>
 * Example for T = "banana":
 * <pre>
 *   r  SA  suffix   lcp
 *   0  5   a        0
 *   1  3   ana      1   ("a")
 *   2  1   anana    3   ("ana")
 *   3  0   banana   0
 *   4  4   na       0
 *   5  2   nana     2   ("na")
 * </pre>
 *
 * <h2>Why Kasai is fast</h2>
 * Comparing every pair of neighbouring suffixes from scratch costs O(n^2) in the worst case.
 * Kasai visits the suffixes in <i>text order</i> (i = 0, 1, 2, ...) and uses one lemma:
 * <blockquote>
 *   If suffix i shares h symbols with the suffix just before it in the Suffix Array,
 *   then suffix i+1 shares at least h-1 symbols with the suffix just before it.
 * </blockquote>
 * (Remove the first symbol from both suffixes: the shorter pair still matches in h-1 symbols,
 * and its predecessor in sorted order can only share more.)
 * So the match length h never restarts from zero. It drops by at most 1 per step and can grow
 * at most n times in total, which gives <b>O(n) time</b>.
 */
public final class KasaiLCP {

    private KasaiLCP() {
    }

    /**
     * Computes the LCP Array.
     *
     * @param text        the text the Suffix Array was built from
     * @param suffixArray the Suffix Array of {@code text}
     * @return lcp where lcp[r] = LCP(suffix SA[r-1], suffix SA[r]) and lcp[0] = 0
     */
    public static int[] compute(int[] text, int[] suffixArray) {
        final int n = text.length;
        if (suffixArray.length != n) {
            throw new IllegalArgumentException("Suffix Array length does not match text length");
        }
        int[] lcp = new int[n];

        // rankOf[i] = position of suffix i inside the Suffix Array (the inverse permutation).
        int[] rankOf = new int[n];
        for (int r = 0; r < n; r++) {
            rankOf[suffixArray[r]] = r;
        }

        int h = 0; // number of symbols already known to match
        for (int i = 0; i < n; i++) {            // visit suffixes in TEXT order
            if (rankOf[i] == 0) {
                h = 0;                           // first suffix in sorted order: no predecessor
                continue;
            }
            int j = suffixArray[rankOf[i] - 1];  // suffix that precedes suffix i in sorted order

            // Extend the match beyond the h symbols we already know are equal.
            while (i + h < n && j + h < n && text[i + h] == text[j + h]) {
                h++;
            }
            lcp[rankOf[i]] = h;

            // Kasai lemma: the next suffix (i + 1) matches its predecessor in at least h - 1 symbols.
            if (h > 0) {
                h--;
            }
        }
        return lcp;
    }

    /** Convenience overload for a {@link SuffixArray} object. */
    public static int[] compute(SuffixArray suffixArray) {
        return compute(suffixArray.getText(), suffixArray.getArray());
    }
}
