package com.docs_similarity.dsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the text shared by every pair of documents, using only the generalized Suffix Array
 * and the LCP Array of the {@link CombinedText}.
 *
 * <h2>Step 1: longest match for every word position (two linear sweeps)</h2>
 * Fix a target document B. For every word-start position p of another document A we want
 * <pre>
 *   matchLength_B(p) = length of the longest prefix of suffix p that also starts at a
 *                      word start somewhere in B
 * </pre>
 * The LCP Array answers this through the <b>range minimum property</b>: for Suffix Array
 * indexes x &lt; y,
 * <pre>
 *   LCP(SA[x], SA[y]) = min(lcp[x+1], lcp[x+2], ..., lcp[y])
 * </pre>
 * The minimum can only shrink when y moves further away from x, so the best partner in B for
 * any suffix is the <i>nearest</i> suffix of B directly above it or directly below it in the
 * Suffix Array. One top-down sweep and one bottom-up sweep, each carrying the running minimum
 * of LCP values since the last suffix of B, compute matchLength_B for all positions in O(n).
 * Repeating this for every target document costs O(k * n) for k documents.
 *
 * <h2>Step 2: common segments</h2>
 * The candidate segment at p is the region [p, p + matchLength_B(p)), shortened so that it ends
 * with a complete word. It is a genuine common substring: the same characters start at a word
 * start in B. A candidate is kept only if it is at least L characters long, where L is the
 * minimum match length (for a document shorter than L, L is lowered to that document's length).
 *
 * <h2>Step 3: shared characters without double counting</h2>
 * Candidates overlap heavily: if "the quick brown fox" matches at p, then "quick brown fox"
 * matches at the next word. We therefore count the <b>union</b> of all kept segments:
 * <pre>
 *   SharedChars(A -&gt; B) = | union of all kept segments of A with respect to B |
 * </pre>
 * Every character of A is counted at most once, no matter how many segments contain it or
 * how many times the text occurs in B. Because candidates are visited by increasing start
 * position, the union is measured in one pass by remembering how far the covered region
 * already reaches ({@code coveredUntil}).
 *
 * <p>A candidate that does not reach past {@code coveredUntil} lies completely inside an earlier
 * segment, so it is not reported on its own. The candidates that remain are exactly the
 * <b>maximal</b> common segments of A, and those are the segments shown to the user.
 */
public final class CommonSubstringAnalyzer {

    /** One maximal common segment, reported from the point of view of document A. */
    public record Segment(int documentA, int documentB, int offsetA, int offsetB, int length) {
    }

    /**
     * Everything found for one unordered pair (A, B) with A &lt; B.
     *
     * @param sharedCharsA       characters of A inside segments shared with B
     * @param sharedCharsB       characters of B inside segments shared with A
     * @param effectiveMinLength minimum segment length actually used for this pair
     * @param segmentCount       number of maximal common segments found in A
     * @param longestSegment     length of the longest common segment
     * @param topSegments        the longest segments (at most the requested limit), longest first
     */
    public record PairMatches(int documentA, int documentB, int sharedCharsA, int sharedCharsB,
                              int effectiveMinLength, int segmentCount, int longestSegment,
                              List<Segment> topSegments) {
    }

    /** Result for all pairs, ordered by (documentA, documentB). */
    public record Result(List<PairMatches> pairs, int totalSegments) {
    }

    private final CombinedText text;
    private final int[] suffixArray;
    private final int[] lcp;

    public CommonSubstringAnalyzer(CombinedText text, int[] suffixArray, int[] lcp) {
        if (suffixArray.length != text.length() || lcp.length != text.length()) {
            throw new IllegalArgumentException("Suffix Array and LCP Array must match the combined text length");
        }
        this.text = text;
        this.suffixArray = suffixArray;
        this.lcp = lcp;
    }

    /**
     * Runs the analysis for every pair of documents.
     *
     * @param minMatchLength     L, the minimum length (in characters) of a common segment
     * @param maxSegmentsPerPair how many of the longest segments to keep per pair for display
     */
    public Result analyze(int minMatchLength, int maxSegmentsPerPair) {
        if (minMatchLength < 1) {
            throw new IllegalArgumentException("Minimum match length must be at least 1");
        }
        final int k = text.documentCount();
        final int n = text.length();

        int[][] sharedChars = new int[k][k];             // [a][b] = chars of a shared with b
        List<List<Segment>> segmentsOfPair = new ArrayList<>(k * k);
        for (int i = 0; i < k * k; i++) {
            segmentsOfPair.add(null);
        }

        int[] matchLength = new int[n];
        int[] matchPosition = new int[n];

        for (int target = 0; target < k; target++) {
            computeLongestMatches(target, matchLength, matchPosition);

            for (int source = 0; source < k; source++) {
                if (source == target) {
                    continue;
                }
                // Segments are listed once per pair, from the lower numbered document's side.
                List<Segment> segments = source < target ? new ArrayList<>() : null;
                sharedChars[source][target] = measureSharedText(source, target,
                        effectiveMinLength(minMatchLength, source, target),
                        matchLength, matchPosition, segments);
                if (segments != null) {
                    segmentsOfPair.set(source * k + target, segments);
                }
            }
        }

        List<PairMatches> pairs = new ArrayList<>();
        int totalSegments = 0;
        for (int a = 0; a < k; a++) {
            for (int b = a + 1; b < k; b++) {
                List<Segment> all = segmentsOfPair.get(a * k + b);
                all.sort(Comparator.comparingInt(Segment::length).reversed()
                        .thenComparingInt(Segment::offsetA));
                int longest = all.isEmpty() ? 0 : all.get(0).length();
                List<Segment> top = List.copyOf(all.subList(0, Math.min(maxSegmentsPerPair, all.size())));
                pairs.add(new PairMatches(a, b, sharedChars[a][b], sharedChars[b][a],
                        effectiveMinLength(minMatchLength, a, b), all.size(), longest, top));
                totalSegments += all.size();
            }
        }
        return new Result(pairs, totalSegments);
    }

    /**
     * L for one pair. A document shorter than L could never produce a segment, so for such a
     * pair L is lowered to the length of the shorter document. This lets a short document still
     * be recognised when it is fully contained in the other one.
     */
    private int effectiveMinLength(int minMatchLength, int a, int b) {
        int shorter = Math.min(text.documentLength(a), text.documentLength(b));
        return Math.max(1, Math.min(minMatchLength, shorter));
    }

    /**
     * Step 1. Fills matchLength[p] and matchPosition[p] for every word-start position p that
     * is NOT in the target document: the longest common prefix of suffix p with any word-start
     * suffix of the target document, and one position in the target where it occurs.
     */
    private void computeLongestMatches(int target, int[] matchLength, int[] matchPosition) {
        final int n = suffixArray.length;
        Arrays.fill(matchLength, 0);
        Arrays.fill(matchPosition, -1);

        // Sweep 1, top to bottom: nearest suffix of the target document ABOVE each suffix.
        int nearestTarget = -1;       // text position of that nearest target suffix, -1 if none yet
        int runningMin = 0;           // min(lcp) between the nearest target suffix and the current row
        for (int r = 0; r < n; r++) {
            if (nearestTarget >= 0) {
                runningMin = Math.min(runningMin, lcp[r]);   // lcp[r] links rows r-1 and r
            }
            int pos = suffixArray[r];
            if (!text.isWordStart(pos)) {
                continue;
            }
            if (text.documentOf(pos) == target) {
                nearestTarget = pos;
                runningMin = Integer.MAX_VALUE;
            } else if (nearestTarget >= 0) {
                matchLength[pos] = runningMin;
                matchPosition[pos] = nearestTarget;
            }
        }

        // Sweep 2, bottom to top: nearest suffix of the target document BELOW each suffix.
        nearestTarget = -1;
        runningMin = Integer.MAX_VALUE;
        for (int r = n - 1; r >= 0; r--) {
            int pos = suffixArray[r];
            if (text.isWordStart(pos)) {
                if (text.documentOf(pos) == target) {
                    nearestTarget = pos;
                    runningMin = Integer.MAX_VALUE;
                } else if (nearestTarget >= 0 && runningMin > matchLength[pos]) {
                    matchLength[pos] = runningMin;
                    matchPosition[pos] = nearestTarget;
                }
            }
            if (nearestTarget >= 0) {
                runningMin = Math.min(runningMin, lcp[r]);   // prepare for row r-1
            }
        }
    }

    /**
     * Steps 2 and 3 for one ordered pair (source, target).
     *
     * @param segments if not null, receives every maximal common segment of the source document
     * @return the number of source characters covered by common segments (each counted once)
     */
    private int measureSharedText(int source, int target, int minLength,
                                  int[] matchLength, int[] matchPosition, List<Segment> segments) {
        final int start = text.documentStart(source);
        final int end = text.separatorPosition(source);
        final int targetStart = text.documentStart(target);

        int shared = 0;
        int coveredUntil = start;   // every character before this position is already counted

        for (int p = start; p < end; p++) {
            if (!text.isWordStart(p) || matchLength[p] < minLength) {
                continue;
            }
            // The match never crosses the separator, so p + matchLength[p] <= end.
            int segmentEnd = text.trimToWordEnd(p, p + matchLength[p]);
            if (segmentEnd - p < minLength) {
                continue;
            }
            if (segmentEnd <= coveredUntil) {
                continue;           // lies completely inside an earlier segment: nothing new
            }
            // Count only the part that is not covered yet, so overlaps are never counted twice.
            shared += segmentEnd - Math.max(p, coveredUntil);
            coveredUntil = segmentEnd;

            if (segments != null) {
                segments.add(new Segment(source, target, p - start,
                        matchPosition[p] - targetStart, segmentEnd - p));
            }
        }
        return shared;
    }
}
