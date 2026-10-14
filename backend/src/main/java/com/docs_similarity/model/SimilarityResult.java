package com.docs_similarity.model;

import java.util.List;

/**
 * The similarity result for one pair of documents, including the
 * distinct common word sequences that back the calculated score.
 */
public class SimilarityResult {

    private int rank;
    private final int docAId;
    private final int docBId;
    private final String docAName;
    private final String docBName;
    private final double similarityPercent;
    private final int wordCountA;
    private final int wordCountB;
    private final int sharedWordsInA;   // words of A inside at least one merged common sequence with B
    private final int sharedWordsInB;   // words of B inside at least one merged common sequence with A
    private final int sharedWords;      // min(sharedWordsInA, sharedWordsInB), used in the formula
    private final int totalCommonSequences; // distinct maximal common word sequences for this pair
    private final List<CommonWordSequence> topCommonSequences; // longest sequences (truncated for display)

    public SimilarityResult(int docAId, int docBId, String docAName, String docBName,
                            double similarityPercent, int wordCountA, int wordCountB,
                            int sharedWordsInA, int sharedWordsInB, int sharedWords,
                            int totalCommonSequences, List<CommonWordSequence> topCommonSequences) {
        this.docAId = docAId;
        this.docBId = docBId;
        this.docAName = docAName;
        this.docBName = docBName;
        this.similarityPercent = similarityPercent;
        this.wordCountA = wordCountA;
        this.wordCountB = wordCountB;
        this.sharedWordsInA = sharedWordsInA;
        this.sharedWordsInB = sharedWordsInB;
        this.sharedWords = sharedWords;
        this.totalCommonSequences = totalCommonSequences;
        this.topCommonSequences = topCommonSequences;
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public int getDocAId() { return docAId; }
    public int getDocBId() { return docBId; }
    public String getDocAName() { return docAName; }
    public String getDocBName() { return docBName; }
    public double getSimilarityPercent() { return similarityPercent; }
    public int getWordCountA() { return wordCountA; }
    public int getWordCountB() { return wordCountB; }
    public int getSharedWordsInA() { return sharedWordsInA; }
    public int getSharedWordsInB() { return sharedWordsInB; }
    public int getSharedWords() { return sharedWords; }
    public int getTotalCommonSequences() { return totalCommonSequences; }
    public List<CommonWordSequence> getTopCommonSequences() { return topCommonSequences; }
}
