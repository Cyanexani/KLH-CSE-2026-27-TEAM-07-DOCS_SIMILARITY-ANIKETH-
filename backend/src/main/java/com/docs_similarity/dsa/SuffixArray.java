package com.docs_similarity.dsa;

/**
 * Suffix Array construction using <b>prefix doubling with radix sort</b>
 * (the Manber and Myers approach).
 *
 * <h2>What is a Suffix Array?</h2>
 * For a text T of length n, the suffix starting at position i is T[i..n-1].
 * The Suffix Array SA is the list of all n starting positions, ordered so that
 * the suffixes appear in lexicographic (dictionary) order.
 *
 * <pre>
 *   T = "banana"            sorted suffixes          SA
 *   0 banana                5  a                     5
 *   1 anana                 3  ana                   3
 *   2 nana         ==&gt;      1  anana                 1
 *   3 ana                   0  banana                0
 *   4 na                    4  na                    4
 *   5 a                     2  nana                  2
 * </pre>
 *
 * <h2>Algorithm: prefix doubling</h2>
 * <ol>
 *   <li>Round 0: sort all suffixes by their first symbol and give every suffix a
 *       <i>rank</i> (equal first symbols get equal ranks).</li>
 *   <li>Round with step k: the rank of suffix i describes its first k symbols.
 *       The first 2k symbols of suffix i are fully described by the pair
 *       (rank[i], rank[i + k]). Sorting all suffixes by that pair therefore sorts
 *       them by their first 2k symbols. If i + k runs past the end of the text the
 *       second key is "empty", which sorts before every real rank.</li>
 *   <li>The pairs are sorted with a two-pass <b>radix sort</b> (a stable counting sort
 *       by the second key, then a stable counting sort by the first key). Each pass
 *       is O(n), because ranks are integers smaller than n.</li>
 *   <li>Stop as soon as every suffix has a different rank. All suffixes are then in
 *       their final order.</li>
 * </ol>
 *
 * <h2>Complexity</h2>
 * Each round costs O(n) and the step k doubles every round, so at most
 * ceil(log2 n) rounds are needed: <b>O(n log n) time</b> and O(n) extra memory.
 *
 * <p>The text is an array of non-negative integers instead of a Java String, so that
 * the analyzer can insert unique separator symbols between documents
 * (see {@link CombinedText}).
 */
public final class SuffixArray {

    private final int[] text;
    private final int[] suffixArray;
    private final int doublingRounds;

    private SuffixArray(int[] text, int[] suffixArray, int doublingRounds) {
        this.text = text;
        this.suffixArray = suffixArray;
        this.doublingRounds = doublingRounds;
    }

    /** Convenience method for plain strings, mainly used for teaching examples and tests. */
    public static SuffixArray fromString(String s) {
        int[] symbols = new int[s.length()];
        for (int i = 0; i < s.length(); i++) {
            symbols[i] = s.charAt(i);
        }
        return build(symbols);
    }

    /**
     * Builds the Suffix Array of {@code text}.
     *
     * @param text symbols of the text, every value must be &gt;= 0
     */
    public static SuffixArray build(int[] text) {
        final int n = text.length;
        if (n == 0) {
            return new SuffixArray(text, new int[0], 0);
        }

        int maxSymbol = 0;
        for (int symbol : text) {
            if (symbol < 0) {
                throw new IllegalArgumentException("Suffix Array symbols must be non-negative, found " + symbol);
            }
            maxSymbol = Math.max(maxSymbol, symbol);
        }

        // order[r] = starting position of the suffix currently placed at sorted position r.
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }

        // ---- Round 0: sort suffixes by their FIRST symbol only ------------------------
        order = countingSortByKey(order, text, maxSymbol + 1);

        // rank[i] = equivalence class of suffix i. Equal prefixes get equal ranks.
        int[] rank = new int[n];
        rank[order[0]] = 0;
        for (int r = 1; r < n; r++) {
            boolean sameFirstSymbol = text[order[r]] == text[order[r - 1]];
            rank[order[r]] = rank[order[r - 1]] + (sameFirstSymbol ? 0 : 1);
        }
        int classCount = rank[order[n - 1]] + 1;

        int[] secondKey = new int[n];
        int[] newRank = new int[n];
        int rounds = 0;

        // ---- Doubling rounds: k = 1, 2, 4, 8, ... -------------------------------------
        // Invariant at the start of a round: suffixes are sorted by their first k symbols
        // and rank[i] identifies the first k symbols of suffix i.
        for (int k = 1; classCount < n; k <<= 1) {
            rounds++;

            // Second key = rank of the suffix that starts k positions later.
            // 0 means "no such suffix" (past the end), so real ranks are shifted by +1.
            for (int i = 0; i < n; i++) {
                secondKey[i] = (i + k < n) ? rank[i + k] + 1 : 0;
            }

            // Radix sort on the pair (rank[i], secondKey[i]):
            // least significant key first, then the most significant key.
            // Both passes are stable, which is what makes radix sort correct.
            order = countingSortByKey(order, secondKey, classCount + 1);
            order = countingSortByKey(order, rank, classCount);

            // Re-rank: two neighbours share a class only if BOTH keys are equal,
            // meaning their first 2k symbols are identical.
            newRank[order[0]] = 0;
            for (int r = 1; r < n; r++) {
                int current = order[r];
                int previous = order[r - 1];
                boolean samePair = rank[current] == rank[previous]
                        && secondKey[current] == secondKey[previous];
                newRank[current] = newRank[previous] + (samePair ? 0 : 1);
            }

            int[] swap = rank;
            rank = newRank;
            newRank = swap;
            classCount = rank[order[n - 1]] + 1;
            // When classCount == n every suffix has a unique rank: the order is final.
        }

        return new SuffixArray(text, order, rounds);
    }

    /**
     * Stable counting sort of suffix positions by an integer key.
     *
     * @param order    suffix positions in their current order
     * @param key      key[i] is the sort key of suffix i, in the range [0, keyRange)
     * @param keyRange number of distinct key values
     * @return a new array with the same positions, sorted by key; ties keep their input order
     */
    private static int[] countingSortByKey(int[] order, int[] key, int keyRange) {
        // bucketStart[v] = how many suffixes have a key smaller than v
        int[] bucketStart = new int[keyRange + 1];
        for (int suffix : order) {
            bucketStart[key[suffix] + 1]++;
        }
        for (int v = 1; v <= keyRange; v++) {
            bucketStart[v] += bucketStart[v - 1];
        }
        int[] sorted = new int[order.length];
        for (int suffix : order) {
            sorted[bucketStart[key[suffix]]++] = suffix;
        }
        return sorted;
    }

    /** The Suffix Array itself. The returned array is shared, callers must not modify it. */
    public int[] getArray() {
        return suffixArray;
    }

    /** The text this Suffix Array was built from. */
    public int[] getText() {
        return text;
    }

    public int length() {
        return suffixArray.length;
    }

    /** Number of doubling rounds that were needed before every suffix had a unique rank. */
    public int getDoublingRounds() {
        return doublingRounds;
    }
}
