package com.docs_similarity.dsa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks document pairs from most similar to least similar.
 *
 * <p>Order used (the first rule that differs decides):
 * <ol>
 *   <li>higher similarity first</li>
 *   <li>more shared characters in total first (larger evidence wins a tie)</li>
 *   <li>lower document numbers first (keeps the output deterministic)</li>
 * </ol>
 * For P pairs the sort costs O(P log P). The sort is stable.
 */
public final class PairRanker {

    public static final Comparator<SimilarityCalculator.PairScore> RANKING_ORDER =
            Comparator.comparingDouble(SimilarityCalculator.PairScore::similarity).reversed()
                    .thenComparing(Comparator.comparingInt(SimilarityCalculator.PairScore::totalSharedChars).reversed())
                    .thenComparingInt(SimilarityCalculator.PairScore::documentA)
                    .thenComparingInt(SimilarityCalculator.PairScore::documentB);

    private PairRanker() {
    }

    /** Returns a new list with the pairs in ranking order. The input list is not modified. */
    public static List<SimilarityCalculator.PairScore> rank(List<SimilarityCalculator.PairScore> scores) {
        List<SimilarityCalculator.PairScore> ranked = new ArrayList<>(scores);
        ranked.sort(RANKING_ORDER);
        return ranked;
    }
}
