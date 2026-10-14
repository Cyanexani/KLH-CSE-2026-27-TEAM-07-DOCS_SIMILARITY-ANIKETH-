package com.docs_similarity.model;

/** One row of the Suffix Array / LCP technical view shown in the "Algorithm Details" section. */
public class SuffixArrayEntry {
    private final int saIndex;          // index within the suffix array
    private final int suffixPosition;   // starting position in the combined token sequence
    private final String documentName;  // which original document this suffix belongs to
    private final int offsetInDocument; // word offset within that original document
    private final int lcp;              // LCP (in words) with the previous suffix in sorted order
    private final String suffixPreview; // first few words of the suffix (from token IDs)

    public SuffixArrayEntry(int saIndex, int suffixPosition, String documentName,
                             int offsetInDocument, int lcp, String suffixPreview) {
        this.saIndex = saIndex;
        this.suffixPosition = suffixPosition;
        this.documentName = documentName;
        this.offsetInDocument = offsetInDocument;
        this.lcp = lcp;
        this.suffixPreview = suffixPreview;
    }

    public int getSaIndex() { return saIndex; }
    public int getSuffixPosition() { return suffixPosition; }
    public String getDocumentName() { return documentName; }
    public int getOffsetInDocument() { return offsetInDocument; }
    public int getLcp() { return lcp; }
    public String getSuffixPreview() { return suffixPreview; }
}
