package com.docs_similarity.extraction;

/**
 * Thrown whenever a document's text cannot be extracted: an unsupported
 * file type, a corrupt or password-protected file, or a file (such as a
 * scanned/image-only PDF) that contains no extractable text at all.
 *
 * This is a CHECKED exception deliberately: every extractor is forced to
 * either return real text or explain clearly why it couldn't, rather than
 * silently returning an empty string that could be mistaken for "analyzed
 * successfully, found nothing in common".
 */
public class ExtractionException extends Exception {
    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
