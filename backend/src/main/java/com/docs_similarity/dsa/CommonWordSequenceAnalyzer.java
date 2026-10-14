package com.docs_similarity.dsa;

import com.docs_similarity.model.CommonWordSequence;
import com.docs_similarity.model.Document;
import com.docs_similarity.util.TokenCombiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CommonWordSequenceAnalyzer
 * ------------------------------------------------------------------
 * Uses the Suffix Array + Kasai LCP Array of the COMBINED WORD TOKEN
 * SEQUENCE of all documents to discover word sequences that appear in
 * DIFFERENT documents. This is the generalized-suffix-array technique
 * for finding shared content between multiple strings, applied to words:
 *
 *   1. Tokenize every document into normalized words, map each word to
 *      an integer token ID, and concatenate the documents' ID sequences,
 *      each followed by a unique sentinel (see TokenCombiner), so a match
 *      can never cross a document boundary.
 *   2. Build the Suffix Array of that integer sequence (all word-suffixes
 *      from every document, sorted together).
 *   3. Build the LCP Array with Kasai's Algorithm. LCP values are numbers
 *      of WORDS.
 *   4. For a suffix s and another document Y, the longest word sequence
 *      starting at s that also occurs somewhere in Y equals the largest
 *      LCP between s and any suffix of Y. In sorted order that is always
 *      achieved by the NEAREST Y-suffix above or below s, and the LCP
 *      between two sorted positions is the minimum of the LCP array
 *      between them. One forward and one backward sweep over the Suffix
 *      Array per document therefore gives, for every suffix, its longest
 *      match in Y and where in Y that match is.
 *
 * Unlike looking only at immediately ADJACENT suffixes, step 4 still finds
 * a match between documents A and C when a third document B's suffix
 * sorts between them (e.g. three copies of the same paragraph).
 *
 * Walking a document's suffixes in order, consecutive matches that stay on
 * the same alignment (offsetInA - offsetInB) overlap: "machine learning
 * algorithms are" at offset 10 and "learning algorithms are" at offset 11
 * are the same shared passage. Those are merged into one run while
 * scanning, so the output is a compact list of word-offset intervals, not
 * one entry per suffix. When two alignments give an equally long match the
 * scan keeps the current alignment, so a passage is not split up for no
 * reason. CommonSequenceMerger finishes the merge per document pair.
 */
public class CommonWordSequenceAnalyzer {

    private final SuffixArray suffixArray;
    private final KasaiLCP kasaiLCP;
    private final TokenCombiner.CombinedTokens combined;
    private final List<Document> documents;
    private final int minSequenceWords;
    private long rawSuffixMatchCount = -1;

    public CommonWordSequenceAnalyzer(List<Document> documents, int minSequenceWords) {
        if (minSequenceWords < 1) {
            throw new IllegalArgumentException("minSequenceWords must be at least 1");
        }
        this.documents = documents;
        this.minSequenceWords = minSequenceWords;
        this.combined = TokenCombiner.combine(documents);
        this.suffixArray = new SuffixArray(combined.tokens);
        this.kasaiLCP = new KasaiLCP(suffixArray);
    }

    public SuffixArray getSuffixArray() {
        return suffixArray;
    }

    public KasaiLCP getKasaiLCP() {
        return kasaiLCP;
    }

    public TokenCombiner.CombinedTokens getCombinedTokens() {
        return combined;
    }

    /**
     * Number of (suffix, other document) pairs whose longest shared word
     * sequence reached the minimum length, i.e. the suffix-level matches
     * before any merging. Available after {@link #findRawMatches()}.
     */
    public long getRawSuffixMatchCount() {
        return rawSuffixMatchCount;
    }

    /**
     * Finds every cross-document shared word sequence of at least
     * minSequenceWords words. Each result is reported with docAId &lt; docBId
     * and word offsets in both documents; text is left empty (the merger
     * rebuilds it from token IDs after merging).
     */
    public List<CommonWordSequence> findRawMatches() {
        int[] sa = suffixArray.getSuffixArray();
        int[] lcp = kasaiLCP.getLCPArray();
        int[] docAt = combined.docIdAtPosition;
        int n = sa.length;

        int[] bestLen = new int[n];
        int[] bestPos = new int[n];
        List<CommonWordSequence> matches = new ArrayList<>();
        long suffixMatches = 0;

        for (Document target : documents) {
            int y = target.getId();
            Arrays.fill(bestLen, 0);
            Arrays.fill(bestPos, -1);

            // Forward sweep: nearest suffix of Y ABOVE each suffix in sorted order.
            int lastY = -1;
            int runMin = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (i > 0) runMin = Math.min(runMin, lcp[i]);
                int p = sa[i];
                int d = docAt[p];
                if (d == y) {
                    lastY = p;
                    runMin = Integer.MAX_VALUE;
                } else if (d >= 0 && lastY >= 0 && runMin > bestLen[p]) {
                    bestLen[p] = runMin;
                    bestPos[p] = lastY;
                }
            }

            // Backward sweep: nearest suffix of Y BELOW each suffix in sorted order.
            int nextY = -1;
            runMin = Integer.MAX_VALUE;
            for (int i = n - 1; i >= 0; i--) {
                int p = sa[i];
                int d = docAt[p];
                if (d == y) {
                    nextY = p;
                    runMin = Integer.MAX_VALUE;
                } else if (d >= 0 && nextY >= 0 && runMin > bestLen[p]) {
                    bestLen[p] = runMin;
                    bestPos[p] = nextY;
                }
                runMin = Math.min(runMin, lcp[i]); // LCP between sorted positions i-1 and i
            }

            // Scan every other document X in word order, turning the per-suffix
            // best matches into runs of shared words.
            for (Document source : documents) {
                int x = source.getId();
                if (x == y) continue;
                suffixMatches += collectRuns(source, y, bestLen, bestPos, matches);
            }
        }

        this.rawSuffixMatchCount = suffixMatches;
        return matches;
    }

    private int collectRuns(Document source, int y, int[] bestLen, int[] bestPos, List<CommonWordSequence> out) {
        int x = source.getId();
        int start = combined.documentStart[x];
        int words = source.wordCount();
        int[] offsetAt = combined.offsetAtPosition;

        int suffixMatches = 0;
        int prevLen = 0;
        int prevPosY = -1;

        int runStartX = -1;
        int runStartY = -1;
        int runEndX = -1;

        for (int off = 0; off < words; off++) {
            int p = start + off;
            int len = bestLen[p];
            int q = bestPos[p];

            // Prefer continuing the previous alignment when it is just as long.
            if (len > 0 && prevPosY >= 0 && prevLen - 1 == len) {
                q = prevPosY + 1;
            }
            prevLen = len;
            prevPosY = len > 0 ? q : -1;

            if (len < minSequenceWords) continue;
            suffixMatches++;

            int offY = offsetAt[q];
            boolean sameAlignment = runStartX >= 0 && (off - offY) == (runStartX - runStartY);
            if (sameAlignment && off <= runEndX) {
                runEndX = Math.max(runEndX, off + len);
            } else {
                if (runStartX >= 0) {
                    out.add(normalized(x, y, runStartX, runStartY, runEndX - runStartX));
                }
                runStartX = off;
                runStartY = offY;
                runEndX = off + len;
            }
        }
        if (runStartX >= 0) {
            out.add(normalized(x, y, runStartX, runStartY, runEndX - runStartX));
        }
        return suffixMatches;
    }

    /** Reports a match with the lower document ID as "A". */
    private static CommonWordSequence normalized(int x, int y, int offX, int offY, int length) {
        if (x < y) {
            return new CommonWordSequence("", length, x, y, offX, offY);
        }
        return new CommonWordSequence("", length, y, x, offY, offX);
    }
}
