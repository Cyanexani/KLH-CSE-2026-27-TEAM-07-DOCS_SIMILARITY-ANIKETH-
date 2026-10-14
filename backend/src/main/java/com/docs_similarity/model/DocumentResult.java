package com.docs_similarity.model;

/** Summary info about one analyzed document, returned to the frontend. */
public class DocumentResult {
    private final int id;
    private final String filename;
    private final int length;              // extracted text length in characters
    private final int wordCount;           // number of words compared
    private final String sourceFormat;     // "TXT" | "PDF" | "DOCX"
    private final String extractionStatus; // "SUCCESS" for every document that reaches this response

    public DocumentResult(int id, String filename, int length, int wordCount, String sourceFormat, String extractionStatus) {
        this.id = id;
        this.filename = filename;
        this.length = length;
        this.wordCount = wordCount;
        this.sourceFormat = sourceFormat;
        this.extractionStatus = extractionStatus;
    }

    public int getId() { return id; }
    public String getFilename() { return filename; }
    public int getLength() { return length; }
    public int getWordCount() { return wordCount; }
    public String getSourceFormat() { return sourceFormat; }
    public String getExtractionStatus() { return extractionStatus; }
}
