package com.docs_similarity.model;

import java.util.List;

/** Full response payload for POST /api/analyze. Everything here is computed by the real DSA engine. */
public class AnalysisResponse {

    private final List<DocumentResult> documents;
    private final List<SimilarityResult> ranking;
    private final Summary summary;
    private final AlgorithmDetails algorithmDetails;
    private final List<String> pipelineStages;

    public AnalysisResponse(List<DocumentResult> documents, List<SimilarityResult> ranking,
                             Summary summary, AlgorithmDetails algorithmDetails, List<String> pipelineStages) {
        this.documents = documents;
        this.ranking = ranking;
        this.summary = summary;
        this.algorithmDetails = algorithmDetails;
        this.pipelineStages = pipelineStages;
    }

    public List<DocumentResult> getDocuments() { return documents; }
    public List<SimilarityResult> getRanking() { return ranking; }
    public Summary getSummary() { return summary; }
    public AlgorithmDetails getAlgorithmDetails() { return algorithmDetails; }
    public List<String> getPipelineStages() { return pipelineStages; }

    /** Top-of-dashboard summary card values — never hardcoded, always derived from the actual run. */
    public static class Summary {
        private final int documentsAnalyzed;
        private final int pairsCompared;
        private final int commonSegmentsFound;
        private final double highestSimilarity;

        public Summary(int documentsAnalyzed, int pairsCompared, int commonSegmentsFound, double highestSimilarity) {
            this.documentsAnalyzed = documentsAnalyzed;
            this.pairsCompared = pairsCompared;
            this.commonSegmentsFound = commonSegmentsFound;
            this.highestSimilarity = highestSimilarity;
        }

        public int getDocumentsAnalyzed() { return documentsAnalyzed; }
        public int getPairsCompared() { return pairsCompared; }
        public int getCommonSegmentsFound() { return commonSegmentsFound; }
        public double getHighestSimilarity() { return highestSimilarity; }
    }
}
