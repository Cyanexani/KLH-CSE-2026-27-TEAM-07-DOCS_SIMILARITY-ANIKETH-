package com.docs_similarity.dsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommonSubstringAnalyzerTest {

    /** Runs the complete DSA pipeline on already normalized strings. */
    static CommonSubstringAnalyzer.Result analyze(int minLength, String... documents) {
        CombinedText text = CombinedText.of(List.of(documents));
        SuffixArray sa = SuffixArray.build(text.symbols());
        int[] lcp = KasaiLCP.compute(sa);
        return new CommonSubstringAnalyzer(text, sa.getArray(), lcp).analyze(minLength, 100);
    }

    static String segmentText(String doc, CommonSubstringAnalyzer.Segment s) {
        return doc.substring(s.offsetA(), s.offsetA() + s.length());
    }

    @Test
    @DisplayName("finds a known shared phrase with its exact position in both documents")
    void findsKnownSharedPhrase() {
        String a = "alpha beta gamma the quick brown fox jumps over the lazy dog delta";
        String b = "one two three the quick brown fox jumps over the lazy dog four five";
        String phrase = "the quick brown fox jumps over the lazy dog";

        CommonSubstringAnalyzer.PairMatches pair = analyze(10, a, b).pairs().get(0);

        assertEquals(1, pair.segmentCount());
        CommonSubstringAnalyzer.Segment s = pair.topSegments().get(0);
        assertEquals(phrase, segmentText(a, s));
        assertEquals(a.indexOf(phrase), s.offsetA());
        assertEquals(b.indexOf(phrase), s.offsetB());
        assertEquals(phrase.length(), pair.sharedCharsA());
        assertEquals(phrase.length(), pair.sharedCharsB());
    }

    @Test
    @DisplayName("unrelated documents share nothing")
    void unrelatedDocuments() {
        CommonSubstringAnalyzer.PairMatches pair = analyze(5, "aaaa bbbb cccc dddd", "wwww xxxx yyyy zzzz").pairs().get(0);
        assertEquals(0, pair.segmentCount());
        assertEquals(0, pair.sharedCharsA());
        assertEquals(0, pair.sharedCharsB());
    }

    @Test
    @DisplayName("overlapping segments are counted once")
    void overlappingSegmentsAreNotDoubleCounted() {
        String a = "a b c d e f g";               // 13 characters
        String b = "a b c d e x c d e f g";       // contains "a b c d e" and "c d e f g" separately
        CommonSubstringAnalyzer.PairMatches pair = analyze(5, a, b).pairs().get(0);

        // Segments "a b c d e" [0,9) and "c d e f g" [4,13) overlap in "c d e".
        assertEquals(2, pair.segmentCount());
        // Union = [0,13) = 13 characters, not 9 + 9 = 18.
        assertEquals(13, pair.sharedCharsA());
        // In b only the "x" and its two spaces are not shared.
        assertEquals(18, pair.sharedCharsB());
    }

    @Test
    @DisplayName("text repeated in one document is counted on that side only")
    void repeatedPhrase() {
        String phrase = "the cat sat on the mat";                   // 22 characters
        String a = phrase + " and " + phrase;                        // 49 characters, phrase twice
        String b = "yes " + phrase;                                  // 26 characters, phrase once
        CommonSubstringAnalyzer.PairMatches pair = analyze(10, a, b).pairs().get(0);
        assertEquals(44, pair.sharedCharsA());
        assertEquals(22, pair.sharedCharsB());
    }

    @Test
    @DisplayName("segments never join text from two different documents")
    void segmentsDoNotCrossDocumentBoundaries() {
        String a = "the end of part one";
        String b = "part two begins here";
        String c = "part one part two begins";   // looks like the end of a followed by the start of b
        CommonSubstringAnalyzer.Result result = analyze(5, a, b, c);

        for (CommonSubstringAnalyzer.PairMatches pair : result.pairs()) {
            for (CommonSubstringAnalyzer.Segment s : pair.topSegments()) {
                assertTrue(s.length() <= "part two begins".length(), "segment crosses a boundary: " + s);
            }
        }
        CommonSubstringAnalyzer.PairMatches ac = result.pairs().get(1);   // (a, c)
        assertEquals("part one", segmentText(a, ac.topSegments().get(0)));
        CommonSubstringAnalyzer.PairMatches bc = result.pairs().get(2);   // (b, c)
        assertEquals("part two begins", segmentText(b, bc.topSegments().get(0)));
    }

    @Test
    @DisplayName("segments start and end on whole words")
    void segmentsAreWordAligned() {
        String a = "reviewing the results of the experiment was slow";
        String b = "testing the results of the experiment went well";
        CommonSubstringAnalyzer.PairMatches pair = analyze(10, a, b).pairs().get(0);
        assertEquals("the results of the experiment", segmentText(a, pair.topSegments().get(0)));
    }

    @Test
    @DisplayName("segments shorter than the minimum length are ignored")
    void minimumLengthIsRespected() {
        String a = "one two three four five six";
        String b = "zero two three nine";
        assertEquals(0, analyze(10, a, b).pairs().get(0).segmentCount());      // "two three" has 9 chars
        assertEquals(9, analyze(9, a, b).pairs().get(0).sharedCharsA());
    }

    @Test
    @DisplayName("shared characters match a brute force definition on random texts")
    void matchesBruteForceCoverage() {
        Random random = new Random(3);
        String[] words = {"ab", "ba", "a", "b", "aab"};
        for (int trial = 0; trial < 150; trial++) {
            String a = randomText(random, words, 1 + random.nextInt(25));
            String b = randomText(random, words, 1 + random.nextInt(25));
            int minLength = 1 + random.nextInt(8);
            CommonSubstringAnalyzer.PairMatches pair = analyze(minLength, a, b).pairs().get(0);
            int effective = Math.min(minLength, Math.min(a.length(), b.length()));
            assertEquals(bruteForceShared(a, b, effective), pair.sharedCharsA(), a + " | " + b);
            assertEquals(bruteForceShared(b, a, effective), pair.sharedCharsB(), b + " | " + a);
        }
    }

    private static String randomText(Random random, String[] words, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(words[random.nextInt(words.length)]);
        }
        return sb.toString();
    }

    /**
     * Direct definition: mark every character of x inside a substring x[i..j) that starts at a word
     * start of x, ends at a word end of x, is at least minLength long and occurs in y starting at a
     * word start of y. Count the marked characters.
     */
    private static int bruteForceShared(String x, String y, int minLength) {
        boolean[] covered = new boolean[x.length()];
        for (int i = 0; i < x.length(); i++) {
            if (x.charAt(i) == ' ' || (i > 0 && x.charAt(i - 1) != ' ')) {
                continue;
            }
            for (int j = i + minLength; j <= x.length(); j++) {
                if (j < x.length() && x.charAt(j) != ' ') {
                    continue;
                }
                String candidate = x.substring(i, j);
                if (occursAtWordStart(y, candidate)) {
                    for (int p = i; p < j; p++) {
                        covered[p] = true;
                    }
                }
            }
        }
        int count = 0;
        for (boolean c : covered) {
            if (c) {
                count++;
            }
        }
        return count;
    }

    private static boolean occursAtWordStart(String y, String candidate) {
        for (int from = y.indexOf(candidate); from >= 0; from = y.indexOf(candidate, from + 1)) {
            if (from == 0 || y.charAt(from - 1) == ' ') {
                return true;
            }
        }
        return false;
    }
}
