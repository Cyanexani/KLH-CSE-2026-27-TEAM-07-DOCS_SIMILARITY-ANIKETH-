package com.docs_similarity.extraction;

/**
 * A single-format text extraction strategy. Implementations convert raw
 * file bytes of one supported format into plain text that can be fed into
 * the existing Suffix Array / Kasai LCP similarity pipeline unchanged.
 *
 * Implementations must never return null; they either return real
 * extracted text or throw ExtractionException with a clear, user-facing
 * explanation of why extraction was not possible.
 */
public interface TextExtractor {
    String extractText(byte[] fileBytes, String filename) throws ExtractionException;
}
