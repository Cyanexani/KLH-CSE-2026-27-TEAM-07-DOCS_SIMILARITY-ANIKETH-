package com.docs_similarity.algo;

/** Module 3: Levenshtein distance by the Wagner-Fischer DP, O(nm) time, O(min(n, m)) memory. */
public final class EditDistance {

    private EditDistance() {
    }

    public static int levenshtein(String a, String b) {
        if (a.length() < b.length()) {
            String t = a;
            a = b;
            b = t;
        }
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int sub = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                cur[j] = Math.min(sub, Math.min(prev[j] + 1, cur[j - 1] + 1));
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    /** 1.0 for identical strings, 0.0 for completely different ones. */
    public static double similarity(String a, String b) {
        int longest = Math.max(a.length(), b.length());
        return longest == 0 ? 1.0 : 1.0 - (double) levenshtein(a, b) / longest;
    }
}
