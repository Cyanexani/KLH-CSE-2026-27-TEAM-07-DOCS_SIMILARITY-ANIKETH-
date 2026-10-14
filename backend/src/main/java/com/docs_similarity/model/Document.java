package com.docs_similarity.model;

import com.docs_similarity.util.WordTokenizer;

import java.util.List;
import java.util.Locale;

/**
 * Represents a single uploaded document after text extraction and
 * preprocessing. The DSA pipeline only ever sees getWords() — the
 * normalized words of the plain text — regardless of whether the
 * original file was TXT, PDF, or DOCX.
 */
public class Document {

    private final int id;
    private final String filename;
    private final String content; // extracted + preprocessed plain text (used in the combined text)
    private final String sourceFormat; // "TXT" | "PDF" | "DOCX" | "UNKNOWN"
    private final List<String> words;  // normalized words, in order (see WordTokenizer)

    /** Convenience constructor that infers the source format from the filename extension. */
    public Document(int id, String filename, String content) {
        this(id, filename, content, inferFormat(filename));
    }

    public Document(int id, String filename, String content, String sourceFormat) {
        this.id = id;
        this.filename = filename;
        this.content = content;
        this.sourceFormat = sourceFormat;
        this.words = List.copyOf(WordTokenizer.tokenize(content));
    }

    private static String inferFormat(String filename) {
        if (filename == null) return "UNKNOWN";
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".txt")) return "TXT";
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".docx")) return "DOCX";
        return "UNKNOWN";
    }

    public int getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContent() {
        return content;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    /** Length of the extracted text in characters. */
    public int length() {
        return content.length();
    }

    public List<String> getWords() {
        return words;
    }

    /** Number of words the document contributes to the token sequence. */
    public int wordCount() {
        return words.size();
    }
}
