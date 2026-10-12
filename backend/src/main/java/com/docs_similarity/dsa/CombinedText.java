package com.docs_similarity.dsa;

import java.util.List;

/**
 * Joins several documents into ONE symbol sequence so that a single (generalized)
 * Suffix Array can index all of them at once:
 *
 * <pre>
 *   T = D1 $1 D2 $2 D3 $3 ... Dk $k
 * </pre>
 *
 * <h2>Unique separators</h2>
 * Every document is followed by its own separator symbol, and each separator occurs exactly
 * once in T. The symbols are integers:
 * <pre>
 *   separator after document d (0-based)  = d + 1              (values 1 .. k)
 *   ordinary character c                  = c + k + 1          (always larger than any separator)
 * </pre>
 *
 * <h2>Why matches can never cross a document boundary</h2>
 * Suppose two different suffixes starting at positions i != j matched each other past a
 * separator. Then at some offset t both would read the SAME separator, so i + t = j + t = the
 * single position of that separator, which forces i = j. Contradiction. Therefore the LCP of two
 * different suffixes always stops before (or at) the first separator, so every common prefix
 * found through the LCP array lies completely inside one document on each side.
 *
 * <p>The class also records, for every position of T, which document it belongs to and the
 * offset inside that document, so the Suffix Array is never treated as one ordinary string.
 */
public final class CombinedText {

    private final int[] symbols;
    private final int[] documentOf;          // position -> document id (separator belongs to the document before it)
    private final int[] documentStart;       // document id -> first position in T
    private final int[] documentLength;      // document id -> number of characters
    private final int[] boundaryAtOrBefore;  // position -> last position <= it holding a space or separator
    private final int documentCount;
    private final int spaceSymbol;
    private final int alphabetSize;

    private CombinedText(int[] symbols, int[] documentOf, int[] documentStart, int[] documentLength,
                         int documentCount) {
        this.symbols = symbols;
        this.documentOf = documentOf;
        this.documentStart = documentStart;
        this.documentLength = documentLength;
        this.documentCount = documentCount;
        this.spaceSymbol = ' ' + documentCount + 1;
        this.boundaryAtOrBefore = computeBoundaries();
        this.alphabetSize = countDistinctSymbols();
    }

    /**
     * Builds the combined text.
     *
     * @param documents the (already preprocessed) documents, in order
     */
    public static CombinedText of(List<String> documents) {
        final int k = documents.size();
        if (k == 0) {
            throw new IllegalArgumentException("At least one document is required");
        }
        int total = k; // one separator per document
        for (String doc : documents) {
            total += doc.length();
        }

        int[] symbols = new int[total];
        int[] documentOf = new int[total];
        int[] documentStart = new int[k];
        int[] documentLength = new int[k];

        int pos = 0;
        for (int d = 0; d < k; d++) {
            String doc = documents.get(d);
            documentStart[d] = pos;
            documentLength[d] = doc.length();
            for (int i = 0; i < doc.length(); i++) {
                symbols[pos] = doc.charAt(i) + k + 1;   // shift characters above all separators
                documentOf[pos] = d;
                pos++;
            }
            symbols[pos] = d + 1;                       // unique separator $ for document d
            documentOf[pos] = d;
            pos++;
        }
        return new CombinedText(symbols, documentOf, documentStart, documentLength, k);
    }

    private int[] computeBoundaries() {
        int[] result = new int[symbols.length];
        int last = -1;
        for (int pos = 0; pos < symbols.length; pos++) {
            if (symbols[pos] == spaceSymbol || isSeparator(pos)) {
                last = pos;
            }
            result[pos] = last;
        }
        return result;
    }

    private int countDistinctSymbols() {
        int max = 0;
        for (int s : symbols) {
            max = Math.max(max, s);
        }
        boolean[] seen = new boolean[max + 1];
        int distinct = 0;
        for (int s : symbols) {
            if (!seen[s]) {
                seen[s] = true;
                distinct++;
            }
        }
        return distinct;
    }

    // ---------------------------------------------------------------------------------------
    // Position bookkeeping
    // ---------------------------------------------------------------------------------------

    /** The integer symbols of T, used as input for the Suffix Array. Callers must not modify it. */
    public int[] symbols() {
        return symbols;
    }

    public int length() {
        return symbols.length;
    }

    public int documentCount() {
        return documentCount;
    }

    public int alphabetSize() {
        return alphabetSize;
    }

    /** Document that owns position {@code pos}. A separator belongs to the document it closes. */
    public int documentOf(int pos) {
        return documentOf[pos];
    }

    public int documentStart(int doc) {
        return documentStart[doc];
    }

    public int documentLength(int doc) {
        return documentLength[doc];
    }

    /** Position of the separator that closes document {@code doc}. */
    public int separatorPosition(int doc) {
        return documentStart[doc] + documentLength[doc];
    }

    public boolean isSeparator(int pos) {
        return pos == separatorPosition(documentOf[pos]);
    }

    /** Offset of {@code pos} inside its own document (0 = first character of the document). */
    public int offsetInDocument(int pos) {
        return pos - documentStart[documentOf[pos]];
    }

    /**
     * A word start is a non-space character that begins its document or follows a space.
     * Only suffixes that start at a word start are used for common substring analysis, so every
     * reported segment begins with a complete word in both documents.
     */
    public boolean isWordStart(int pos) {
        if (isSeparator(pos) || symbols[pos] == spaceSymbol) {
            return false;
        }
        return pos == documentStart[documentOf[pos]] || symbols[pos - 1] == spaceSymbol;
    }

    /**
     * Shortens the region [start, end) so that it finishes at the end of a complete word.
     * A region may end at position e when T[e] is a space or a separator.
     *
     * @return the new (exclusive) end, or {@code start} if the region contains no complete word
     */
    public int trimToWordEnd(int start, int end) {
        int boundary = boundaryAtOrBefore[end];
        return boundary > start ? boundary : start;
    }

    // ---------------------------------------------------------------------------------------
    // Converting symbols back to readable text
    // ---------------------------------------------------------------------------------------

    /** Characters of T in [from, from + length). Must not contain a separator. */
    public String text(int from, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int pos = from; pos < from + length; pos++) {
            if (isSeparator(pos)) {
                throw new IllegalArgumentException("Region crosses a document separator at " + pos);
            }
            sb.append((char) (symbols[pos] - documentCount - 1));
        }
        return sb.toString();
    }

    /**
     * Human readable preview of the suffix starting at {@code pos}. Separators are shown as
     * {@code $1}, {@code $2}, ... (1-based document number) so the viewer can see where a
     * document ends inside the combined text.
     */
    public String preview(int pos, int maxSymbols) {
        StringBuilder sb = new StringBuilder();
        int end = Math.min(symbols.length, pos + maxSymbols);
        for (int p = pos; p < end; p++) {
            if (isSeparator(p)) {
                sb.append('$').append(documentOf[p] + 1);
            } else {
                sb.append((char) (symbols[p] - documentCount - 1));
            }
        }
        return sb.toString();
    }
}
