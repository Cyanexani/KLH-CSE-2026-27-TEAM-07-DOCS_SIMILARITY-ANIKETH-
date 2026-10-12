package com.docs_similarity.model;

import java.util.List;

/** Complete result of {@code POST /api/analyze}. */
public record AnalysisResponse(String analysisId, AnalysisParameters parameters,
                               List<DocumentResult> documents, AnalysisSummary summary,
                               List<AnalysisStage> stages, List<SimilarityResult> ranking,
                               double[][] similarityMatrix, AlgorithmInfo algorithm) {
}
