package com.docs_similarity.dsa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuffixArrayTest {

    @Test
    @DisplayName("banana: textbook example")
    void banana() {
        // a(5) ana(3) anana(1) banana(0) na(4) nana(2)
        assertArrayEquals(new int[]{5, 3, 1, 0, 4, 2}, SuffixArray.fromString("banana").getArray());
    }

    @Test
    @DisplayName("mississippi: repeated substrings")
    void mississippi() {
        assertArrayEquals(new int[]{10, 7, 4, 1, 0, 9, 8, 6, 3, 5, 2},
                SuffixArray.fromString("mississippi").getArray());
    }

    @Test
    @DisplayName("a run of equal characters: shorter suffixes come first")
    void allEqualCharacters() {
        assertArrayEquals(new int[]{3, 2, 1, 0}, SuffixArray.fromString("aaaa").getArray());
    }

    @Test
    void singleCharacterAndEmptyText() {
        assertArrayEquals(new int[]{0}, SuffixArray.fromString("x").getArray());
        assertArrayEquals(new int[0], SuffixArray.fromString("").getArray());
    }

    @Test
    @DisplayName("integer alphabet with separators: separators sort first")
    void integerSymbolsWithSeparators() {
        // "ab$1ab$2" with $1 = 1, $2 = 2 and a = 10, b = 11
        int[] text = {10, 11, 1, 10, 11, 2};
        // suffixes: $1ab$2(2) $2(5) ab$1ab$2(0) ab$2(3) b$1ab$2(1) b$2(4)
        assertArrayEquals(new int[]{2, 5, 0, 3, 1, 4}, SuffixArray.build(text).getArray());
    }

    @Test
    void rejectsNegativeSymbols() {
        assertThrows(IllegalArgumentException.class, () -> SuffixArray.build(new int[]{1, -1}));
    }

    @Test
    @DisplayName("stops doubling as soon as all ranks are unique")
    void doublingRoundsAreLogarithmic() {
        assertEquals(0, SuffixArray.fromString("abcd").getDoublingRounds());   // first symbols already unique
        SuffixArray sa = SuffixArray.fromString("a".repeat(1024));
        assertTrue(sa.getDoublingRounds() <= 11, "log2(1024) + 1 rounds at most");
    }

    @Test
    @DisplayName("matches a brute force sort on 500 random strings")
    void matchesNaiveSortOnRandomStrings() {
        Random random = new Random(42);
        for (int trial = 0; trial < 500; trial++) {
            int length = 1 + random.nextInt(200);
            int alphabet = 1 + random.nextInt(4);   // small alphabets create many repeats
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append((char) ('a' + random.nextInt(alphabet)));
            }
            String s = sb.toString();
            assertArrayEquals(naiveSuffixArray(s), SuffixArray.fromString(s).getArray(), "text: " + s);
        }
    }

    /** O(n^2 log n) reference: sort the suffixes as strings. */
    static int[] naiveSuffixArray(String s) {
        Integer[] positions = new Integer[s.length()];
        for (int i = 0; i < s.length(); i++) {
            positions[i] = i;
        }
        Arrays.sort(positions, (a, b) -> s.substring(a).compareTo(s.substring(b)));
        return Arrays.stream(positions).mapToInt(Integer::intValue).toArray();
    }
}
