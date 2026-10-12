package com.docs_similarity.algo;

/** Module 2: Knuth-Morris-Pratt exact matching in O(n + m) using the failure function. */
public final class Kmp {

    private Kmp() {
    }

    /** failure[i] = length of the longest proper prefix of p[0..i] that is also its suffix. */
    public static int[] failure(String p) {
        int[] f = new int[p.length()];
        for (int i = 1, k = 0; i < p.length(); i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) {
                k = f[k - 1];
            }
            if (p.charAt(i) == p.charAt(k)) {
                k++;
            }
            f[i] = k;
        }
        return f;
    }

    /** Start positions of every (possibly overlapping) occurrence of pattern in text. */
    public static int[] search(String text, String pattern) {
        if (pattern.isEmpty()) {
            return new int[0];
        }
        int[] f = failure(pattern);
        int[] hits = new int[text.length() + 1];
        int count = 0;
        for (int i = 0, k = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) {
                k = f[k - 1];
            }
            if (text.charAt(i) == pattern.charAt(k)) {
                k++;
            }
            if (k == pattern.length()) {
                hits[count++] = i - k + 1;
                k = f[k - 1];
            }
        }
        int[] out = new int[count];
        System.arraycopy(hits, 0, out, 0, count);
        return out;
    }
}
