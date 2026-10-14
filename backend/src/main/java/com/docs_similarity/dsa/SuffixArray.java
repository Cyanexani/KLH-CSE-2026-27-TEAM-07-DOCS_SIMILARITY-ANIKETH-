package com.docs_similarity.dsa;

import java.util.Arrays;

/**
 * SuffixArray
 * ------------------------------------------------------------------
 * Genuine construction of the Suffix Array of a SEQUENCE OF INTEGER
 * SYMBOLS using the classic "prefix doubling" technique (Manber &amp;
 * Myers style), running in O(n log n) time.
 *
 * In this project every symbol is a WORD: each normalized word of the
 * documents is mapped to an integer token ID (see util.TokenCombiner),
 * and the Suffix Array is built over that integer token sequence. A
 * "suffix" is therefore "all words from position i to the end", and
 * suffixes are sorted word-by-word (lexicographically by token ID).
 *
 * The algorithm itself does not care what a symbol means, so the same
 * class also accepts a plain String (each character becomes one symbol).
 * That convenience constructor keeps the classic textbook example
 * checkable. For the text "banana$":
 *
 *   index  suffix
 *   0      banana$
 *   1      anana$
 *   2      nana$
 *   3      ana$
 *   4      na$
 *   5      a$
 *   6      $
 *
 * Sorted lexicographically the Suffix Array is: [6, 5, 3, 1, 0, 4, 2]
 * because "$" &lt; "a$" &lt; "ana$" &lt; "anana$" &lt; "banana$" &lt; "na$" &lt; "nana$"
 *
 * WHY PREFIX DOUBLING?
 * A naive approach sorts all n suffixes directly with a comparator,
 * costing O(n^2 log n) in the worst case (each comparison can take
 * O(n)). Prefix doubling instead sorts suffixes by their first
 * 2^k symbols in round k, reusing the ranks from round k-1 so each
 * suffix is described by a pair of integers (rank of its first half,
 * rank of its second half). Each round sorts those pairs with a
 * two-pass counting (radix) sort in O(n), and after O(log n) rounds
 * every suffix is distinguished, giving O(n log n) total.
 */
public class SuffixArray {

    private final int[] tokens;
    private final int n;
    private final int[] suffixArray;   // suffixArray[i] = starting index of the i-th smallest suffix
    private final int[] rank;          // rank[i] = rank of the suffix starting at i (inverse of suffixArray)

    /** Builds the Suffix Array over an integer symbol sequence (word token IDs in this project). */
    public SuffixArray(int[] tokens) {
        this.tokens = tokens;
        this.n = tokens.length;
        this.suffixArray = build();
        this.rank = invert(this.suffixArray);
    }

    /** Convenience constructor: every character of the string is one symbol. */
    public SuffixArray(String text) {
        this(text.chars().toArray());
    }

    /** The symbol sequence this Suffix Array was built over. */
    public int[] getTokens() {
        return tokens;
    }

    public int length() {
        return n;
    }

    /** The constructed suffix array: suffixArray[i] = start index of i-th lexicographically smallest suffix. */
    public int[] getSuffixArray() {
        return suffixArray;
    }

    /** rank[i] = position of the suffix starting at index i within the sorted suffix array. */
    public int[] getRank() {
        return rank;
    }

    private static int[] invert(int[] sa) {
        int n = sa.length;
        int[] inv = new int[n];
        for (int i = 0; i < n; i++) {
            inv[sa[i]] = i;
        }
        return inv;
    }

    /**
     * Core prefix-doubling construction.
     *
     * rankArr[i] holds the rank of the suffix starting at i, based on the
     *            first k symbols compared so far (equal prefixes share a rank).
     * Each round sorts suffixes by the pair (rankArr[i], rankArr[i + k]),
     * where a second half that runs past the end of the sequence sorts first
     * (a shorter suffix is a smaller suffix). The pair sort is an LSD radix
     * sort: a stable counting sort on the second key, then on the first.
     */
    private int[] build() {
        int n = this.n;
        if (n == 0) {
            return new int[0];
        }

        // Round 0: rank each suffix by its first symbol. Symbols (token IDs,
        // sentinels, characters) are compressed to dense ranks 0..distinct-1
        // so the counting sorts below work with arbitrary int values.
        int[] distinct = Arrays.stream(tokens).distinct().sorted().toArray();
        int[] rankArr = new int[n];
        for (int i = 0; i < n; i++) {
            rankArr[i] = Arrays.binarySearch(distinct, tokens[i]);
        }
        int classes = distinct.length;

        int[] sa = new int[n];
        int[] bySecondKey = new int[n];
        int[] newRank = new int[n];

        for (int k = 1; ; k <<= 1) {
            // Second key is shifted by +1 so that "past the end" (0) sorts first.
            int buckets = classes + 1;
            int[] count = new int[buckets + 1];

            // Pass 1: stable counting sort of all positions by the second key.
            for (int i = 0; i < n; i++) {
                count[secondKey(rankArr, i, k, n)]++;
            }
            prefixSums(count);
            for (int i = n - 1; i >= 0; i--) {
                bySecondKey[--count[secondKey(rankArr, i, k, n)]] = i;
            }

            // Pass 2: stable counting sort by the first key (the current rank).
            Arrays.fill(count, 0);
            for (int i = 0; i < n; i++) {
                count[rankArr[i]]++;
            }
            prefixSums(count);
            for (int idx = n - 1; idx >= 0; idx--) {
                int i = bySecondKey[idx];
                sa[--count[rankArr[i]]] = i;
            }

            // Recompute ranks for the next round based on the new order.
            newRank[sa[0]] = 0;
            for (int j = 1; j < n; j++) {
                int cur = sa[j];
                int prev = sa[j - 1];
                boolean samePair = rankArr[cur] == rankArr[prev]
                        && secondKey(rankArr, cur, k, n) == secondKey(rankArr, prev, k, n);
                newRank[cur] = newRank[prev] + (samePair ? 0 : 1);
            }
            System.arraycopy(newRank, 0, rankArr, 0, n);
            classes = rankArr[sa[n - 1]] + 1;

            // Early exit: every suffix already has a unique rank.
            if (classes == n || k >= n) {
                break;
            }
        }

        return sa;
    }

    private static int secondKey(int[] rankArr, int i, int k, int n) {
        return (i + k < n) ? rankArr[i + k] + 1 : 0;
    }

    /** Turns bucket counts into bucket END positions (filled right-to-left for stability). */
    private static void prefixSums(int[] count) {
        for (int b = 1; b < count.length; b++) {
            count[b] += count[b - 1];
        }
    }
}
