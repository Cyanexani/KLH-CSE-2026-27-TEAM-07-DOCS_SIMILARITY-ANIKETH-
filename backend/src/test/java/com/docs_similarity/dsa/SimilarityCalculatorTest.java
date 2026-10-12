package com.docs_similarity.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimilarityCalculatorTest {

    private static final double EPS = 1e-9;

    /** Full pipeline: returns the similarity (0..1) of the two documents. */
    static double similarityOf(int minLength, String a, String b) {
        CombinedText text = CombinedText.of(List.of(a, b));
        SuffixArray sa = SuffixArray.build(text.symbols());
        CommonSubstringAnalyzer.PairMatches pair = new CommonSubstringAnalyzer(text, sa.getArray(),
                KasaiLCP.compute(sa)).analyze(minLength, 10).pairs().get(0);
        return SimilarityCalculator.score(pair, a.length(), b.length()).similarity();
    }

    @Test
    @DisplayName("identical documents: 100%")
    void identicalDocuments() {
        String doc = "suffix arrays store the sorted order of all suffixes of a text and "
                + "together with the lcp array they answer many string questions quickly";
        assertEquals(1.0, similarityOf(25, doc, doc), EPS);
    }

    @Test
    @DisplayName("identical short documents are still 100% (L is lowered to the document length)")
    void identicalShortDocuments() {
        assertEquals(1.0, similarityOf(25, "hello world", "hello world"), EPS);
    }

    @Test
    @DisplayName("completely different documents: 0%")
    void completelyDifferentDocuments() {
        assertEquals(0.0, similarityOf(10,
                "mountains rivers and valleys shape the land",
                "quantum computing uses qubits for calculation"), EPS);
    }

    @Test
    @DisplayName("partial overlap: exact value from the formula")
    void partialOverlap() {
        String shared = "the quick brown fox jumps over the lazy dog";      // 43 characters
        String a = shared + " while the farmer sleeps";                     // 43 + 24 = 67
        String b = "yesterday morning " + shared;                           // 18 + 43 = 61
        // (43 + 43) / (67 + 61) = 86 / 128
        assertEquals(86.0 / 128.0, similarityOf(20, a, b), EPS);
    }

    @Test
    @DisplayName("more shared text gives a higher score")
    void monotonic() {
        String base = "data structures and algorithms are the foundation of efficient software design";
        String half = "data structures and algorithms are the foundation of cooking good food at home";
        String none = "the weather today is sunny with a light breeze from the west over the hills";
        double high = similarityOf(15, base, base);
        double middle = similarityOf(15, base, half);
        double low = similarityOf(15, base, none);
        assertTrue(high > middle && middle > low, high + " > " + middle + " > " + low);
    }

    @Test
    @DisplayName("formula: (sharedA + sharedB) / (|A| + |B|), symmetric and bounded")
    void formula() {
        assertEquals(0.5, SimilarityCalculator.similarity(10, 10, 20, 20), EPS);
        assertEquals((30.0 + 10.0) / (40 + 100), SimilarityCalculator.similarity(30, 10, 40, 100), EPS);
        assertEquals(SimilarityCalculator.similarity(30, 10, 40, 100),
                SimilarityCalculator.similarity(10, 30, 100, 40), EPS);
        assertEquals(1.0, SimilarityCalculator.similarity(40, 100, 40, 100), EPS);
        assertEquals(0.25, SimilarityCalculator.coverage(5, 20), EPS);
        assertThrows(IllegalArgumentException.class, () -> SimilarityCalculator.similarity(21, 0, 20, 20));
    }

    @Test
    @DisplayName("a paragraph repeated in one document never pushes the score above 100%")
    void repeatedContentStaysBounded() {
        String p = "the water cycle moves water between oceans air and land";
        String a = p;
        String b = p + " and again " + p + " and again " + p;
        double s = similarityOf(20, a, b);
        assertTrue(s > 0.9 && s <= 1.0, "similarity " + s);
    }
}
