package com.docs_similarity.service;

import com.docs_similarity.model.CommonWordSequence;
import com.docs_similarity.model.Document;
import com.docs_similarity.util.TokenCombiner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommonSequenceMergerTest {

    // Document A: 20 words w0 .. w19
    private final TokenCombiner.CombinedTokens combined = TokenCombiner.combine(List.of(
            new Document(0, "a.txt", "w0 w1 w2 w3 w4 w5 w6 w7 w8 w9 w10 w11 w12 w13 w14 w15 w16 w17 w18 w19"),
            new Document(1, "b.txt", "unused")));

    private static CommonWordSequence raw(int offsetInA, int offsetInB, int words) {
        return new CommonWordSequence("", words, 0, 1, offsetInA, offsetInB);
    }

    @Test
    void mergesOverlappingMatchesOnSameDiagonalIntoOneSequence() {
        // Both on diagonal offsetInA - offsetInB = 2; A-intervals [2,10) and [6,14) overlap.
        List<CommonWordSequence> merged = CommonSequenceMerger.mergeOnDiagonals(
                List.of(raw(2, 0, 8), raw(6, 4, 8)), combined);

        assertEquals(1, merged.size(), "Overlapping matches on the same diagonal must merge into one sequence");
        assertEquals(2, merged.get(0).getOffsetInA());
        assertEquals(0, merged.get(0).getOffsetInB());
        assertEquals(12, merged.get(0).getWordCount()); // [2,14)
        assertEquals("w2 w3 w4 w5 w6 w7 w8 w9 w10 w11 w12 w13", merged.get(0).getText());
    }

    @Test
    void nestedSuffixMatchesCollapseToTheLongest() {
        // What a 5-word shared passage looks like before merging: one match per suffix.
        List<CommonWordSequence> merged = CommonSequenceMerger.mergeOnDiagonals(
                List.of(raw(3, 10, 5), raw(4, 11, 4), raw(5, 12, 3)), combined);
        assertEquals(1, merged.size());
        assertEquals(5, merged.get(0).getWordCount());
    }

    @Test
    void adjacentMatchesOnSameDiagonalAlsoMerge() {
        List<CommonWordSequence> merged = CommonSequenceMerger.mergeOnDiagonals(
                List.of(raw(0, 0, 5), raw(5, 5, 5)), combined);

        assertEquals(1, merged.size());
        assertEquals(10, merged.get(0).getWordCount());
    }

    @Test
    void keepsMatchesOnDifferentDiagonalsSeparate() {
        List<CommonWordSequence> merged = CommonSequenceMerger.distinctMaximalSequences(
                List.of(raw(0, 10, 3), raw(12, 50, 3)), combined);

        assertEquals(2, merged.size(), "Distinct occurrences on different diagonals must stay separate");
    }

    @Test
    void dropsVariationsContainedInALongerSequence() {
        // [4,7) in A lies inside [2,10) in A, on another diagonal: a fragment, not a new sequence.
        List<CommonWordSequence> distinct = CommonSequenceMerger.distinctMaximalSequences(
                List.of(raw(2, 0, 8), raw(4, 30, 3)), combined);

        assertEquals(1, distinct.size());
        assertEquals(8, distinct.get(0).getWordCount());
    }

    @Test
    void emptyInputProducesEmptyOutput() {
        assertTrue(CommonSequenceMerger.distinctMaximalSequences(List.of(), combined).isEmpty());
    }

    @Test
    void sequencesAreSortedLongestFirst() {
        List<CommonWordSequence> merged = CommonSequenceMerger.distinctMaximalSequences(
                List.of(raw(0, 100, 3), raw(10, 200, 10)), combined);

        assertEquals(10, merged.get(0).getWordCount(), "Longest sequence should come first");
        assertEquals(3, merged.get(1).getWordCount());
    }
}
