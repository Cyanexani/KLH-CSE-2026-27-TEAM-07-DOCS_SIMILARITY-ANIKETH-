package com.docs_similarity.dsa;

import com.docs_similarity.model.Document;
import com.docs_similarity.util.TokenCombiner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KasaiLCPTest {

    @Test
    void testKnownLCPArray_banana() {
        // Suffix array for "banana$": [6, 5, 3, 1, 0, 4, 2]
        // Sorted suffixes: $, a$, ana$, anana$, banana$, na$, nana$
        // LCP with previous: [0, 0, 1, 3, 0, 0, 2]
        SuffixArray sa = new SuffixArray("banana$");
        KasaiLCP kasai = new KasaiLCP(sa);
        int[] expected = {0, 0, 1, 3, 0, 0, 2};
        assertArrayEquals(expected, kasai.getLCPArray());
    }

    @Test
    void testLCPFirstEntryAlwaysZero() {
        SuffixArray sa = new SuffixArray("mississippi$");
        KasaiLCP kasai = new KasaiLCP(sa);
        assertEquals(0, kasai.getLCPArray()[0]);
    }

    @Test
    void testLCPIsMeasuredInWords() {
        // "machine learning algorithms" occurs in both documents: 3 shared WORDS (27 characters).
        Document d0 = new Document(0, "a.txt", "Modern machine learning algorithms");
        Document d1 = new Document(1, "b.txt", "machine learning algorithms improve");
        TokenCombiner.CombinedTokens combined = TokenCombiner.combine(List.of(d0, d1));

        SuffixArray sa = new SuffixArray(combined.tokens);
        int[] lcp = new KasaiLCP(sa).getLCPArray();

        int rankOfA = sa.getRank()[combined.documentStart[0] + 1]; // "machine ..." in doc A
        int rankOfB = sa.getRank()[combined.documentStart[1]];     // "machine ..." in doc B
        assertEquals(1, Math.abs(rankOfA - rankOfB), "Both 'machine learning algorithms' suffixes sort together");
        assertEquals(3, lcp[Math.max(rankOfA, rankOfB)], "LCP counts 3 words, not 27 characters");

        int max = 0;
        for (int v : lcp) max = Math.max(max, v);
        assertEquals(3, max);
    }

    @Test
    void testLCPMatchesActualCommonPrefixLength() {
        String text = "mississippi$";
        SuffixArray sa = new SuffixArray(text);
        KasaiLCP kasai = new KasaiLCP(sa);
        int[] saArr = sa.getSuffixArray();
        int[] lcp = kasai.getLCPArray();

        for (int i = 1; i < saArr.length; i++) {
            String a = text.substring(saArr[i - 1]);
            String b = text.substring(saArr[i]);
            int actualLcp = 0;
            int max = Math.min(a.length(), b.length());
            while (actualLcp < max && a.charAt(actualLcp) == b.charAt(actualLcp)) {
                actualLcp++;
            }
            assertEquals(actualLcp, lcp[i], "Mismatch at SA index " + i);
        }
    }

    @Test
    void testWordLCPMatchesBruteForceOnRandomTokenSequences() {
        Random random = new Random(7);
        for (int trial = 0; trial < 50; trial++) {
            int n = 2 + random.nextInt(80);
            int[] tokens = new int[n];
            for (int i = 0; i < n; i++) tokens[i] = random.nextInt(3);
            SuffixArray sa = new SuffixArray(tokens);
            int[] saArr = sa.getSuffixArray();
            int[] lcp = new KasaiLCP(sa).getLCPArray();
            for (int i = 1; i < n; i++) {
                int a = saArr[i - 1], b = saArr[i], h = 0;
                while (a + h < n && b + h < n && tokens[a + h] == tokens[b + h]) h++;
                assertEquals(h, lcp[i]);
            }
        }
    }
}
