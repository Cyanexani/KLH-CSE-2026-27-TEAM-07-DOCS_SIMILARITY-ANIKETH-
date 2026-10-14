package com.docs_similarity.model;

/**
 * A common word sequence shared by two documents, discovered via the
 * Suffix Array / Kasai LCP analysis. Offsets and length are in WORDS.
 */
public class CommonWordSequence {

    private final String text;      // the shared words, rebuilt from token IDs ("" for raw matches)
    private final int wordCount;
    private final int docAId;
    private final int docBId;
    private final int offsetInA;    // word offset in document A
    private final int offsetInB;    // word offset in document B

    public CommonWordSequence(String text, int wordCount, int docAId, int docBId, int offsetInA, int offsetInB) {
        this.text = text;
        this.wordCount = wordCount;
        this.docAId = docAId;
        this.docBId = docBId;
        this.offsetInA = offsetInA;
        this.offsetInB = offsetInB;
    }

    public String getText() { return text; }
    public int getWordCount() { return wordCount; }
    public int getDocAId() { return docAId; }
    public int getDocBId() { return docBId; }
    public int getOffsetInA() { return offsetInA; }
    public int getOffsetInB() { return offsetInB; }
}
