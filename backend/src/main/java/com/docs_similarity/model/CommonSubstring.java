package com.docs_similarity.model;

/**
 * One maximal common segment of a document pair.
 *
 * @param text      the shared text (normalized), shortened for display when {@code truncated}
 * @param length    full length of the segment in characters
 * @param offsetA   start offset inside document A (normalized text)
 * @param offsetB   start offset of one occurrence inside document B (normalized text)
 * @param lineA     line of the original file of document A where the segment starts
 * @param lineB     line of the original file of document B where the occurrence starts
 */
public record CommonSubstring(String text, int length, int offsetA, int offsetB,
                              int lineA, int lineB, boolean truncated) {
}
