package com.docs_similarity.dsa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KasaiLCPTest {

    @Test
    @DisplayName("banana: LCP = [0, 1, 3, 0, 0, 2]")
    void banana() {
        // a | ana (1) | anana (3) | banana (0) | na (0) | nana (2)
        assertArrayEquals(new int[]{0, 1, 3, 0, 0, 2}, KasaiLCP.compute(SuffixArray.fromString("banana")));
    }

    @Test
    @DisplayName("mississippi: LCP = [0, 1, 1, 4, 0, 0, 1, 0, 2, 1, 3]")
    void mississippi() {
        assertArrayEquals(new int[]{0, 1, 1, 4, 0, 0, 1, 0, 2, 1, 3},
                KasaiLCP.compute(SuffixArray.fromString("mississippi")));
    }

    @Test
    void allEqualCharacters() {
        // a | aa | aaa | aaaa
        assertArrayEquals(new int[]{0, 1, 2, 3}, KasaiLCP.compute(SuffixArray.fromString("aaaa")));
    }

    @Test
    @DisplayName("matches a brute force LCP on 500 random strings")
    void matchesNaiveLcpOnRandomStrings() {
        Random random = new Random(7);
        for (int trial = 0; trial < 500; trial++) {
            int length = 1 + random.nextInt(200);
            int alphabet = 1 + random.nextInt(3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append((char) ('a' + random.nextInt(alphabet)));
            }
            String s = sb.toString();
            SuffixArray sa = SuffixArray.fromString(s);
            int[] lcp = KasaiLCP.compute(sa);
            assertArrayEquals(naiveLcp(s, sa.getArray()), lcp, "text: " + s);
            assertTrue(SuffixArrayVerifier.verify(sa.getText(), sa.getArray(), lcp));
        }
    }

    @Test
    @DisplayName("the verifier rejects a wrong order and a wrong LCP value")
    void verifierDetectsErrors() {
        SuffixArray sa = SuffixArray.fromString("banana");
        int[] lcp = KasaiLCP.compute(sa);
        int[] swapped = {3, 5, 1, 0, 4, 2};
        assertTrue(!SuffixArrayVerifier.verify(sa.getText(), swapped, lcp));
        int[] wrongLcp = {0, 1, 2, 0, 0, 2};
        assertTrue(!SuffixArrayVerifier.verify(sa.getText(), sa.getArray(), wrongLcp));
    }

    static int[] naiveLcp(String s, int[] sa) {
        int[] lcp = new int[sa.length];
        for (int r = 1; r < sa.length; r++) {
            int a = sa[r - 1];
            int b = sa[r];
            int h = 0;
            while (a + h < s.length() && b + h < s.length() && s.charAt(a + h) == s.charAt(b + h)) {
                h++;
            }
            lcp[r] = h;
        }
        return lcp;
    }
}
