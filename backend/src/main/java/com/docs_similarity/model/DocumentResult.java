package com.docs_similarity.model;

/**
 * Per document information.
 *
 * @param id              0-based document number used everywhere in the response
 * @param originalLength  characters in the uploaded text
 * @param processedLength characters after preprocessing (this is |D| in the similarity formula)
 * @param combinedStart   first position of the document inside the combined text
 * @param separatorIndex  position of the unique separator that closes the document
 */
public record DocumentResult(int id, String name, int originalLength, int processedLength,
                             int wordCount, int lineCount, int combinedStart, int separatorIndex) {
}
