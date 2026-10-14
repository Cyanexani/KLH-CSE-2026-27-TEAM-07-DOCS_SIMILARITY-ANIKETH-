package com.docs_similarity.dsa;

import com.docs_similarity.model.Document;
import com.docs_similarity.util.TokenCombiner;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SuffixArrayTest {

    // ---- classic character examples (each char is one symbol) ----

    @Test
    void testKnownSuffixArray_banana() {
        // "banana$" -> classic textbook example
        SuffixArray sa = new SuffixArray("banana$");
        int[] expected = {6, 5, 3, 1, 0, 4, 2};
        assertArrayEquals(expected, sa.getSuffixArray());
    }

    @Test
    void testSuffixesAreLexicographicallySorted() {
        String text = "mississippi$";
        SuffixArray sa = new SuffixArray(text);
        int[] arr = sa.getSuffixArray();

        for (int i = 1; i < arr.length; i++) {
            String prev = text.substring(arr[i - 1]);
            String cur = text.substring(arr[i]);
            assertTrue(prev.compareTo(cur) < 0,
                    "Suffix at index " + (i - 1) + " should sort before index " + i);
        }
    }

    @Test
    void testSingleCharacter() {
        SuffixArray sa = new SuffixArray("a");
        assertArrayEquals(new int[]{0}, sa.getSuffixArray());
    }

    @Test
    void testAllSameCharacters() {
        // "aaaa$" - suffixes: a$, aa$, aaa$, aaaa$, $  -> sorted: $, a$, aa$, aaa$, aaaa$
        SuffixArray sa = new SuffixArray("aaaa$");
        int[] expected = {4, 3, 2, 1, 0};
        assertArrayEquals(expected, sa.getSuffixArray());
    }

    @Test
    void testRankIsInverseOfSuffixArray() {
        SuffixArray sa = new SuffixArray("banana$");
        int[] arr = sa.getSuffixArray();
        int[] rank = sa.getRank();
        for (int i = 0; i < arr.length; i++) {
            assertEquals(i, rank[arr[i]]);
        }
    }

    // ---- token (word ID) suffix arrays ----

    @Test
    void testTokenSuffixArraySortsWordSuffixes() {
        // Words: the(0) cat(1) sat(2) on(3) the(0) mat(4), then the document sentinel -1.
        Document doc = new Document(0, "a.txt", "The cat sat on the mat.");
        TokenCombiner.CombinedTokens combined = TokenCombiner.combine(List.of(doc));
        assertArrayEquals(new int[]{0, 1, 2, 3, 0, 4, -1}, combined.tokens);

        SuffixArray sa = new SuffixArray(combined.tokens);
        // Sorted by token IDs: [-1], [0 1 2 3 0 4 -1], [0 4 -1], [1 ...], [2 ...], [3 ...], [4 -1]
        assertArrayEquals(new int[]{6, 0, 4, 1, 2, 3, 5}, sa.getSuffixArray());
        assertEquals(combined.length(), sa.length(), "One suffix per WORD (plus sentinel), not per character");
    }

    @Test
    void testTokenSuffixArrayMatchesBruteForceOnRandomSequences() {
        Random random = new Random(42);
        for (int trial = 0; trial < 50; trial++) {
            int n = 1 + random.nextInt(60);
            int[] tokens = new int[n];
            for (int i = 0; i < n; i++) {
                tokens[i] = random.nextInt(4) - (random.nextInt(10) == 0 ? 5 : 0); // includes negative sentinel-like IDs
            }
            int[] expected = bruteForce(tokens);
            assertArrayEquals(expected, new SuffixArray(tokens).getSuffixArray(),
                    "Mismatch for " + Arrays.toString(tokens));
        }
    }

    private static int[] bruteForce(int[] tokens) {
        Integer[] idx = new Integer[tokens.length];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> {
            while (a < tokens.length && b < tokens.length) {
                if (tokens[a] != tokens[b]) return Integer.compare(tokens[a], tokens[b]);
                a++;
                b++;
            }
            return Integer.compare(tokens.length - a, tokens.length - b); // shorter suffix first
        });
        return Arrays.stream(idx).mapToInt(Integer::intValue).toArray();
    }
}
