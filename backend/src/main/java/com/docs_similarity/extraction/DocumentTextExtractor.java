package com.docs_similarity.extraction;

import java.util.Locale;
import java.util.Map;

/**
 * Entry point of the extraction layer. Picks the right TextExtractor
 * based on file extension, runs it, and turns any format-specific edge
 * case (unsupported type, no extractable text, scanned/image-only PDF)
 * into a single, clear ExtractionException. Everything past this class
 * — preprocessing, the Suffix Array / Kasai LCP pipeline, similarity
 * scoring — sees only plain text and has no notion of the original
 * file format.
 */
public class DocumentTextExtractor {

    public static final String FORMAT_TXT = "TXT";
    public static final String FORMAT_PDF = "PDF";
    public static final String FORMAT_DOCX = "DOCX";

    private static final Map<String, TextExtractor> EXTRACTORS_BY_EXTENSION = Map.of(
            "txt", new TxtTextExtractor(),
            "pdf", new PdfTextExtractor(),
            "docx", new DocxTextExtractor()
    );

    private static final Map<String, String> FORMAT_LABELS_BY_EXTENSION = Map.of(
            "txt", FORMAT_TXT,
            "pdf", FORMAT_PDF,
            "docx", FORMAT_DOCX
    );

    /** Human-readable list used in validation/error messages. */
    public static final String SUPPORTED_FORMATS_DESCRIPTION = "TXT, PDF, and DOCX";

    /**
     * Extracts plain text from a document's raw bytes based on its filename
     * extension. Throws ExtractionException — never returns a misleading
     * empty result — for unsupported extensions, unreadable/corrupt files,
     * or files with no extractable text (e.g. a scanned, image-only PDF).
     */
    public String extract(byte[] fileBytes, String filename) throws ExtractionException {
        String extension = extensionOf(filename);
        TextExtractor extractor = EXTRACTORS_BY_EXTENSION.get(extension);

        if (extractor == null) {
            throw new ExtractionException(
                    "Unsupported file type: '" + filename + "'. Supported formats are " +
                    SUPPORTED_FORMATS_DESCRIPTION + ".");
        }

        String text = extractor.extractText(fileBytes, filename);

        if (text == null || text.isBlank()) {
            if ("pdf".equals(extension)) {
                throw new ExtractionException(
                        "No extractable text was found in '" + filename + "'. This looks like a scanned or " +
                        "image-only PDF — OCR is not supported in this version, so it can't be analyzed.");
            }
            throw new ExtractionException("Document '" + filename + "' has no usable text content.");
        }

        return text;
    }

    /** The format label (TXT/PDF/DOCX) for a filename, or "UNKNOWN" if unsupported. */
    public String formatOf(String filename) {
        return FORMAT_LABELS_BY_EXTENSION.getOrDefault(extensionOf(filename), "UNKNOWN");
    }

    public boolean isSupported(String filename) {
        return EXTRACTORS_BY_EXTENSION.containsKey(extensionOf(filename));
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
