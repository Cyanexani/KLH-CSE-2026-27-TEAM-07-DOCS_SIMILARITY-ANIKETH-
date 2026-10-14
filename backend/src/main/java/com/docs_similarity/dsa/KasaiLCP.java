package com.docs_similarity.dsa;

/**
 * KasaiLCP
 * ------------------------------------------------------------------
 * Genuine implementation of Kasai's Algorithm for building the
 * LCP (Longest Common Prefix) array from a Suffix Array in O(n) time.
 *
 * LCP[i] = length of the longest common prefix between the suffix
 *          at suffixArray[i] and the suffix at suffixArray[i-1]
 *          (i.e. between consecutive suffixes in sorted order).
 * LCP[0] is conventionally 0 (no predecessor).
 *
 * The Suffix Array in this project is built over WORD token IDs, so
 * the prefix is compared token by token and LCP[i] is a NUMBER OF
 * WORDS: LCP[i] = 3 means the two neighbouring suffixes start with the
 * same three words.
 *
 * WHY KASAI'S ALGORITHM?
 * Computing each LCP[i] directly by comparing full suffixes costs
 * O(n) per pair, giving O(n^2) overall. Kasai's insight is that as
 * we walk through the ORIGINAL sequence order (not sorted order), the
 * LCP length can only decrease by at most 1 between consecutive
 * original positions. This "at most drop by 1" property means the
 * total amount of token comparison work across the whole sequence
 * is bounded by O(n), because every increment of the shared match
 * length is "paid for" once and only decremented gradually.
 *
 * Algorithm:
 *  1. Compute rank[] = inverse of the suffix array (rank[i] = position
 *     of suffix i in the sorted suffix array). SuffixArray already
 *     provides this.
 *  2. Walk i = 0 .. n-1 over the sequence in original order.
 *  3. If rank[i] &gt; 0, let j = suffixArray[rank[i]-1] (the suffix that
 *     sits immediately before suffix i in sorted order).
 *  4. Extend the match length h from where it left off (never resetting
 *     to 0 unnecessarily) while tokens[i+h] == tokens[j+h].
 *  5. LCP[rank[i]] = h.
 *  6. Decrease h by 1 (but not below 0) before moving to i+1.
 */
public class KasaiLCP {

    private final int[] lcp;

    public KasaiLCP(SuffixArray suffixArray) {
        this.lcp = build(suffixArray);
    }

    public int[] getLCPArray() {
        return lcp;
    }

    private int[] build(SuffixArray sa) {
        int[] tokens = sa.getTokens();
        int n = sa.length();
        int[] suffixArr = sa.getSuffixArray();
        int[] rank = sa.getRank();

        int[] lcp = new int[n];
        int h = 0;

        for (int i = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = suffixArr[rank[i] - 1];

                while (i + h < n && j + h < n && tokens[i + h] == tokens[j + h]) {
                    h++;
                }

                lcp[rank[i]] = h;

                if (h > 0) {
                    h--;
                }
            } else {
                // First suffix in sorted order has no predecessor to compare against
                h = 0;
            }
        }

        return lcp;
    }
}
