package com.docs_similarity.extraction;

import java.nio.charset.StandardCharsets;

/**
 * Plain-text extraction: the file's bytes ARE the document, decoded as UTF-8.
 * This preserves the original (pre-multi-format) behavior exactly for .txt files.
 */
public class TxtTextExtractor implements TextExtractor {
    @Override
    public String extractText(byte[] fileBytes, String filename) throws ExtractionException {
        try {
            return new String(fileBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ExtractionException("'" + filename + "' could not be read as a text file: " + e.getMessage(), e);
        }
    }
}
