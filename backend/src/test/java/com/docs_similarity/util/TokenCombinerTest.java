package com.docs_similarity.util;

import com.docs_similarity.model.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenCombinerTest {

    @Test
    void sameWordGetsSameIdAcrossDocumentsAndReverseMappingWorks() {
        Document d0 = new Document(0, "a.txt", "Solar power grows");
        Document d1 = new Document(1, "b.txt", "wind power grows too");
        TokenCombiner.CombinedTokens c = TokenCombiner.combine(List.of(d0, d1));

        // solar=0 power=1 grows=2 | wind=3 power=1 grows=2 too=4
        assertArrayEquals(new int[]{0, 1, 2, -1, 3, 1, 2, 4, -2}, c.tokens);
        assertEquals(List.of("solar", "power", "grows", "wind", "too"), c.vocabulary);
        assertEquals("power grows too", c.wordsOf(1, 1, 3));
    }

    @Test
    void everyDocumentGetsAUniqueSentinelThatIsNeverAWordId() {
        Document d0 = new Document(0, "a.txt", "same text");
        Document d1 = new Document(1, "b.txt", "same text");
        Document d2 = new Document(2, "c.txt", "same text");
        TokenCombiner.CombinedTokens c = TokenCombiner.combine(List.of(d0, d1, d2));

        assertEquals(-1, c.tokens[2]);
        assertEquals(-2, c.tokens[5]);
        assertEquals(-3, c.tokens[8]);
        for (int p = 0; p < c.length(); p++) {
            if (c.isSentinel(p)) {
                assertEquals(-1, c.docIdAtPosition[p]);
            } else {
                assertTrue(c.tokens[p] >= 0);
            }
        }
    }

    @Test
    void positionsMapBackToDocumentAndWordOffset() {
        Document d0 = new Document(0, "a.txt", "alpha beta");
        Document d1 = new Document(1, "b.txt", "gamma delta epsilon");
        TokenCombiner.CombinedTokens c = TokenCombiner.combine(List.of(d0, d1));

        assertEquals(3, c.documentStart[1]);
        assertEquals(1, c.docIdAtPosition[5]);
        assertEquals(2, c.offsetAtPosition[5]);
        assertEquals("epsilon", c.wordAt(5));
    }
}
