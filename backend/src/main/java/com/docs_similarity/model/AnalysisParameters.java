package com.docs_similarity.model;

/** Parameters that were used for an analysis run. */
public record AnalysisParameters(int minMatchLength, int maxSegmentsPerPair, int segmentPreviewChars) {
}
