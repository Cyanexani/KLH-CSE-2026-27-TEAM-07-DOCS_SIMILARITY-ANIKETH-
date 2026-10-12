package com.docs_similarity.model;

/** Headline numbers for the dashboard cards. Every value is computed by the analysis. */
public record AnalysisSummary(int documentsAnalyzed, int pairsCompared, int commonSegmentsFound,
                              double highestSimilarity, String highestPairA, String highestPairB,
                              int combinedTextLength, double totalTimeMs) {
}
