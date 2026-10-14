package com.docs_similarity.service;

import com.docs_similarity.model.CommonWordSequence;
import com.docs_similarity.util.TokenCombiner;

import java.util.*;

/**
 * CommonSequenceMerger
 * ------------------------------------------------------------------
 * Turns the raw, suffix-derived matches of ONE document pair into the
 * distinct, maximal common word sequences that are scored and displayed.
 *
 * WHAT THIS SOLVES
 * A shared passage of k words is found once for (roughly) every word it
 * contains, because every suffix inside the passage finds its partner in
 * the other document: "machine learning algorithms", "learning
 * algorithms", ... Showing all of them, or summing their lengths, would
 * repeat and double-count the same words.
 *
 * STEP 1: DIAGONAL MERGING
 * Two matches describe the SAME aligned passage if they lie on the same
 * "diagonal" (offsetInA - offsetInB is identical) AND their word intervals
 * overlap or touch. Within a diagonal, such intervals are merged into one
 * maximal interval. Because both matches are exact word-for-word matches on
 * the same alignment, the merged interval is also an exact match, and its
 * words are rebuilt from document A's token IDs via the reverse ID -&gt; word
 * mapping. Matches on different diagonals are different alignments (e.g. the
 * same phrase appearing twice) and stay separate.
 *
 * STEP 2: DROP CONTAINED VARIATIONS
 * A merged sequence whose word interval in A lies inside a longer kept
 * sequence's interval in A, or whose interval in B lies inside a longer kept
 * sequence's interval in B, is only a fragment of that longer sequence
 * (typically a repeated phrase matched at another alignment) and is not
 * shown separately.
 *
 * Scoring counts words from the union of the merged intervals (see
 * DocumentSimilarityService), so each shared word is counted once no matter
 * how many matches cover it.
 */
public class CommonSequenceMerger {

    private CommonSequenceMerger() {
    }

    /** Step 1 only: every maximal interval per diagonal (used for word coverage). */
    public static List<CommonWordSequence> mergeOnDiagonals(List<CommonWordSequence> rawMatches,
                                                            TokenCombiner.CombinedTokens combined) {
        if (rawMatches.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<CommonWordSequence>> byDiagonal = new TreeMap<>();
        for (CommonWordSequence m : rawMatches) {
            int diagonal = m.getOffsetInA() - m.getOffsetInB();
            byDiagonal.computeIfAbsent(diagonal, k -> new ArrayList<>()).add(m);
        }

        List<CommonWordSequence> merged = new ArrayList<>();

        for (Map.Entry<Integer, List<CommonWordSequence>> entry : byDiagonal.entrySet()) {
            int diagonal = entry.getKey();
            List<CommonWordSequence> onDiagonal = entry.getValue();
            onDiagonal.sort(Comparator.comparingInt(CommonWordSequence::getOffsetInA));

            int docAId = onDiagonal.get(0).getDocAId();
            int docBId = onDiagonal.get(0).getDocBId();

            int curStart = onDiagonal.get(0).getOffsetInA();
            int curEnd = curStart + onDiagonal.get(0).getWordCount();

            for (int i = 1; i < onDiagonal.size(); i++) {
                CommonWordSequence m = onDiagonal.get(i);
                int start = m.getOffsetInA();
                int end = start + m.getWordCount();

                if (start <= curEnd) {
                    // Overlapping or directly adjacent on the same diagonal -> merge
                    curEnd = Math.max(curEnd, end);
                } else {
                    merged.add(buildSequence(combined, curStart, curEnd, diagonal, docAId, docBId));
                    curStart = start;
                    curEnd = end;
                }
            }
            merged.add(buildSequence(combined, curStart, curEnd, diagonal, docAId, docBId));
        }

        merged.sort(LONGEST_FIRST);
        return merged;
    }

    /** Steps 1 and 2: the distinct maximal common word sequences, longest first. */
    public static List<CommonWordSequence> distinctMaximalSequences(List<CommonWordSequence> rawMatches,
                                                                    TokenCombiner.CombinedTokens combined) {
        return dropContained(mergeOnDiagonals(rawMatches, combined));
    }

    /** Step 2, on sequences already sorted longest first. */
    static List<CommonWordSequence> dropContained(List<CommonWordSequence> sortedLongestFirst) {
        List<CommonWordSequence> kept = new ArrayList<>();
        for (CommonWordSequence candidate : sortedLongestFirst) {
            boolean contained = false;
            for (CommonWordSequence k : kept) {
                if (inside(candidate.getOffsetInA(), candidate.getWordCount(), k.getOffsetInA(), k.getWordCount())
                        || inside(candidate.getOffsetInB(), candidate.getWordCount(), k.getOffsetInB(), k.getWordCount())) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    private static boolean inside(int start, int len, int outerStart, int outerLen) {
        return start >= outerStart && start + len <= outerStart + outerLen;
    }

    private static final Comparator<CommonWordSequence> LONGEST_FIRST =
            Comparator.comparingInt(CommonWordSequence::getWordCount).reversed()
                    .thenComparingInt(CommonWordSequence::getOffsetInA)
                    .thenComparingInt(CommonWordSequence::getOffsetInB);

    private static CommonWordSequence buildSequence(TokenCombiner.CombinedTokens combined, int start, int end,
                                                    int diagonal, int docAId, int docBId) {
        String text = combined.wordsOf(docAId, start, end - start);
        int offsetInB = start - diagonal;
        return new CommonWordSequence(text, end - start, docAId, docBId, start, offsetInB);
    }
}
