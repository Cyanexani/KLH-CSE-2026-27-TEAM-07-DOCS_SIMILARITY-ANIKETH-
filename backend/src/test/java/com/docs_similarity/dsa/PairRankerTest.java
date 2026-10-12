package com.docs_similarity.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.docs_similarity.dsa.SimilarityCalculator.PairScore;

class PairRankerTest {

    private static PairScore score(int a, int b, int sharedA, int sharedB, int lengthA, int lengthB) {
        CommonSubstringAnalyzer.PairMatches m = new CommonSubstringAnalyzer.PairMatches(
                a, b, sharedA, sharedB, 25, 0, 0, List.of());
        return SimilarityCalculator.score(m, lengthA, lengthB);
    }

    @Test
    @DisplayName("pairs are sorted from highest to lowest similarity")
    void sortsBySimilarityDescending() {
        List<PairScore> input = List.of(
                score(0, 1, 20, 20, 100, 100),   // 20%
                score(0, 2, 90, 90, 100, 100),   // 90%
                score(1, 2, 50, 50, 100, 100),   // 50%
                score(0, 3, 0, 0, 100, 100));    // 0%
        List<PairScore> ranked = PairRanker.rank(input);

        assertEquals(List.of(0.9, 0.5, 0.2, 0.0), ranked.stream().map(PairScore::similarity).toList());
        for (int i = 1; i < ranked.size(); i++) {
            assertTrue(ranked.get(i - 1).similarity() >= ranked.get(i).similarity());
        }
        assertEquals(20, input.get(0).matches().sharedCharsA(), "input list is not modified");
    }

    @Test
    @DisplayName("ties: more shared text first, then lower document numbers")
    void tieBreaking() {
        PairScore small = score(2, 3, 10, 10, 20, 20);    // 50%, 20 shared characters
        PairScore large = score(1, 3, 100, 100, 200, 200); // 50%, 200 shared characters
        PairScore zeroA = score(1, 2, 0, 0, 10, 10);
        PairScore zeroB = score(0, 4, 0, 0, 10, 10);
        List<PairScore> ranked = PairRanker.rank(List.of(small, zeroA, large, zeroB));
        assertEquals(List.of(large, small, zeroB, zeroA), ranked);
    }
}
