package com.docs_similarity.model;

import java.util.List;

/**
 * A ranked document pair. Percentages are between 0 and 100.
 *
 * <p>similarity = (sharedCharsA + sharedCharsB) / (lengthA + lengthB) x 100,
 * coverageA = sharedCharsA / lengthA x 100 and coverageB = sharedCharsB / lengthB x 100.
 *
 * @param segmentCount number of maximal common segments found for the pair
 * @param segments     the longest segments (limited), longest first
 */
public record SimilarityResult(int rank, int documentAId, String documentA, int documentBId, String documentB,
                               double similarity, int sharedCharsA, int sharedCharsB,
                               int lengthA, int lengthB, double coverageA, double coverageB,
                               int effectiveMinMatchLength, int segmentCount, int longestSegmentLength,
                               List<CommonSubstring> segments) {
}
