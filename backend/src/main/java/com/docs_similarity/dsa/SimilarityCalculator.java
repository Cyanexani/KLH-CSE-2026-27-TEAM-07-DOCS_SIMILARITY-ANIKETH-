package com.docs_similarity.dsa;

/**
 * The similarity formula used by DOCS_SIMILARITY.
 *
 * <h2>Definitions</h2>
 * <pre>
 *   |A|            number of characters of document A after preprocessing
 *   Shared(A, B)   number of characters of A that lie inside at least one common segment
 *                  with B (computed by {@link CommonSubstringAnalyzer}, each character
 *                  counted once, see "union" in that class)
 * </pre>
 *
 * <h2>Formula</h2>
 * <pre>
 *                     Shared(A, B) + Shared(B, A)
 *   Similarity(A,B) = ---------------------------  x 100 %
 *                            |A| + |B|
 * </pre>
 *
 * <h2>Why this formula</h2>
 * <ul>
 *   <li>It is the familiar Dice style measure 2 x SharedContent / (|A| + |B|) where
 *       SharedContent is the average of the shared amounts measured inside A and inside B.
 *       When every common segment occurs once in each document, Shared(A, B) = Shared(B, A)
 *       and the formula is exactly 2 x Shared / (|A| + |B|).</li>
 *   <li>Shared text is measured separately on each side because the two amounts can differ:
 *       a paragraph copied twice into B covers twice as many characters of B as of A.
 *       Plugging one "shared length" into 2 x Shared / (|A| + |B|) would either count that
 *       paragraph twice or pick one side arbitrarily, and could exceed 100 %.</li>
 *   <li>Range: 0 &lt;= Shared(A, B) &lt;= |A| and 0 &lt;= Shared(B, A) &lt;= |B|, so the score is always
 *       between 0 % and 100 %. It is 100 % for identical documents and 0 % when no common
 *       segment of at least L characters exists. It is symmetric: Similarity(A,B) = Similarity(B,A).</li>
 * </ul>
 *
 * <p>The one-sided value Shared(A, B) / |A| is also reported as the <b>coverage</b> of A:
 * the fraction of A that also appears in B. It is useful for plagiarism checks where a short
 * document is copied into a much longer one.
 */
public final class SimilarityCalculator {

    private SimilarityCalculator() {
    }

    /** Similarity of one pair as a fraction between 0.0 and 1.0. */
    public static double similarity(int sharedCharsA, int sharedCharsB, int lengthA, int lengthB) {
        if (sharedCharsA < 0 || sharedCharsB < 0 || sharedCharsA > lengthA || sharedCharsB > lengthB) {
            throw new IllegalArgumentException("Shared characters must lie between 0 and the document length");
        }
        int totalLength = lengthA + lengthB;
        if (totalLength == 0) {
            return 0.0;
        }
        return (double) (sharedCharsA + sharedCharsB) / totalLength;
    }

    /** Fraction of a document (0.0 to 1.0) that also appears in the other document. */
    public static double coverage(int sharedChars, int length) {
        return length == 0 ? 0.0 : (double) sharedChars / length;
    }

    /** Scores one pair found by the {@link CommonSubstringAnalyzer}. */
    public static PairScore score(CommonSubstringAnalyzer.PairMatches matches, int lengthA, int lengthB) {
        return new PairScore(matches,
                similarity(matches.sharedCharsA(), matches.sharedCharsB(), lengthA, lengthB),
                coverage(matches.sharedCharsA(), lengthA),
                coverage(matches.sharedCharsB(), lengthB),
                lengthA, lengthB);
    }

    /** A scored document pair. All fractions are between 0.0 and 1.0. */
    public record PairScore(CommonSubstringAnalyzer.PairMatches matches, double similarity,
                            double coverageA, double coverageB, int lengthA, int lengthB) {

        public int documentA() {
            return matches.documentA();
        }

        public int documentB() {
            return matches.documentB();
        }

        public int totalSharedChars() {
            return matches.sharedCharsA() + matches.sharedCharsB();
        }
    }
}
