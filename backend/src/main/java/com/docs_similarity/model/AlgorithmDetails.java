package com.docs_similarity.model;

import java.util.List;

/** Metadata about the DSA pipeline run, for the "Algorithm Details" UI section. All lengths are in words/tokens. */
public class AlgorithmDetails {
    private final int documentsAnalyzed;
    private final int combinedTokenCount;       // words of all documents + one sentinel per document
    private final int vocabularySize;           // distinct word token IDs
    private final boolean suffixArrayBuilt;
    private final boolean lcpArrayBuilt;
    private final boolean kasaiAlgorithmUsed;
    private final long rawSuffixMatches;        // suffix-level cross-document matches before merging
    private final int totalCommonSequencesFound; // distinct maximal common word sequences, all pairs
    private final int documentPairsCompared;
    private final int minSequenceWords;
    private final int suffixArrayTotalEntries;
    private final List<SuffixArrayEntry> suffixArraySample; // limited subset

    public AlgorithmDetails(int documentsAnalyzed, int combinedTokenCount, int vocabularySize,
                            boolean suffixArrayBuilt, boolean lcpArrayBuilt, boolean kasaiAlgorithmUsed,
                            long rawSuffixMatches, int totalCommonSequencesFound, int documentPairsCompared,
                            int minSequenceWords, int suffixArrayTotalEntries,
                            List<SuffixArrayEntry> suffixArraySample) {
        this.documentsAnalyzed = documentsAnalyzed;
        this.combinedTokenCount = combinedTokenCount;
        this.vocabularySize = vocabularySize;
        this.suffixArrayBuilt = suffixArrayBuilt;
        this.lcpArrayBuilt = lcpArrayBuilt;
        this.kasaiAlgorithmUsed = kasaiAlgorithmUsed;
        this.rawSuffixMatches = rawSuffixMatches;
        this.totalCommonSequencesFound = totalCommonSequencesFound;
        this.documentPairsCompared = documentPairsCompared;
        this.minSequenceWords = minSequenceWords;
        this.suffixArrayTotalEntries = suffixArrayTotalEntries;
        this.suffixArraySample = suffixArraySample;
    }

    public int getDocumentsAnalyzed() { return documentsAnalyzed; }
    public int getCombinedTokenCount() { return combinedTokenCount; }
    public int getVocabularySize() { return vocabularySize; }
    public boolean isSuffixArrayBuilt() { return suffixArrayBuilt; }
    public boolean isLcpArrayBuilt() { return lcpArrayBuilt; }
    public boolean isKasaiAlgorithmUsed() { return kasaiAlgorithmUsed; }
    public long getRawSuffixMatches() { return rawSuffixMatches; }
    public int getTotalCommonSequencesFound() { return totalCommonSequencesFound; }
    public int getDocumentPairsCompared() { return documentPairsCompared; }
    public int getMinSequenceWords() { return minSequenceWords; }
    public int getSuffixArrayTotalEntries() { return suffixArrayTotalEntries; }
    public List<SuffixArrayEntry> getSuffixArraySample() { return suffixArraySample; }
}
