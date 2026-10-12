package com.docs_similarity.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CombinedTextTest {

    @Test
    @DisplayName("tracks document id, offset and separator for every position")
    void positionBookkeeping() {
        CombinedText t = CombinedText.of(List.of("ab", "cab"));
        // positions: a b $1 c a b $2
        assertEquals(7, t.length());
        assertEquals(0, t.documentStart(0));
        assertEquals(3, t.documentStart(1));
        assertEquals(2, t.separatorPosition(0));
        assertEquals(6, t.separatorPosition(1));
        assertTrue(t.isSeparator(2));
        assertTrue(t.isSeparator(6));
        assertFalse(t.isSeparator(3));
        assertEquals(1, t.documentOf(4));
        assertEquals(1, t.offsetInDocument(4));
        assertEquals("cab", t.text(3, 3));
        assertEquals("b$1cab$2", t.preview(1, 20));
    }

    @Test
    @DisplayName("each separator is unique and smaller than every character")
    void separatorsAreUnique() {
        CombinedText t = CombinedText.of(List.of("x", "x", "x"));
        int[] s = t.symbols();
        assertEquals(1, s[1]);
        assertEquals(2, s[3]);
        assertEquals(3, s[5]);
        assertTrue(s[0] > 3);
    }

    @Test
    @DisplayName("LCP never extends across a document boundary")
    void lcpStopsAtSeparators() {
        // Without separators, "abc" + "abc" would give a repeat of length 3 that continues into the next document.
        CombinedText t = CombinedText.of(List.of("abc", "abc", "abc"));
        SuffixArray sa = SuffixArray.build(t.symbols());
        int[] lcp = KasaiLCP.compute(sa);
        for (int r = 1; r < lcp.length; r++) {
            int a = sa.getArray()[r - 1];
            int b = sa.getArray()[r];
            int endA = t.separatorPosition(t.documentOf(a));
            int endB = t.separatorPosition(t.documentOf(b));
            assertTrue(a + lcp[r] <= endA && b + lcp[r] <= endB, "LCP crosses a separator at row " + r);
        }
        assertEquals(3, java.util.Arrays.stream(lcp).max().orElse(0));
    }

    @Test
    void wordStartsAndWordEnds() {
        CombinedText t = CombinedText.of(List.of("the cat sat", "on mat"));
        assertTrue(t.isWordStart(0));    // t of "the"
        assertFalse(t.isWordStart(1));
        assertFalse(t.isWordStart(3));   // space
        assertTrue(t.isWordStart(4));    // c of "cat"
        assertTrue(t.isWordStart(12));   // o of "on", first character of document 2
        assertFalse(t.isWordStart(11));  // separator
        // "the cat sa" is cut back to "the cat"
        assertEquals(7, t.trimToWordEnd(0, 10));
        // "the cat sat" already ends at the separator
        assertEquals(11, t.trimToWordEnd(0, 11));
        // "th" contains no complete word
        assertEquals(0, t.trimToWordEnd(0, 2));
    }
}
