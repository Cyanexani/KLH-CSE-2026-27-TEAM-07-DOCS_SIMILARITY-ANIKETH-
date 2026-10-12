package com.docs_similarity.model;

import java.util.List;

/** Suffix Array and LCP Array of a short string, row by row. */
public record DemoResponse(String text, int length, int doublingRounds, int[] suffixArray, int[] lcp,
                           List<Row> rows) {

    public record Row(int saIndex, int suffixPosition, String suffix, int lcp) {
    }
}
