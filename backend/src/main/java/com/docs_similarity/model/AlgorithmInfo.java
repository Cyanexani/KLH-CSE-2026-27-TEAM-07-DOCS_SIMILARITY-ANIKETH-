package com.docs_similarity.model;

/**
 * Facts about the data structures built for this analysis.
 *
 * @param combinedTextLength      n, length of D1 $1 D2 $2 ... Dk $k
 * @param alphabetSize            distinct symbols in the combined text (separators included)
 * @param separatorCount          number of unique separators (one per document)
 * @param doublingRounds          prefix doubling rounds needed by the Suffix Array construction
 * @param suffixArrayVerified     result of the linear time consistency check
 * @param maxLcp                  largest value in the LCP Array
 * @param averageLcp              mean of the LCP Array
 * @param crossDocumentNeighbours adjacent Suffix Array rows from different documents with LCP &gt;= L
 */
public record AlgorithmInfo(int combinedTextLength, int alphabetSize, int separatorCount,
                            String suffixArrayAlgorithm, String suffixArrayComplexity,
                            int doublingRounds, boolean suffixArrayVerified,
                            String lcpAlgorithm, String lcpComplexity, int maxLcp, double averageLcp,
                            int crossDocumentNeighbours, String similarityFormula) {
}
