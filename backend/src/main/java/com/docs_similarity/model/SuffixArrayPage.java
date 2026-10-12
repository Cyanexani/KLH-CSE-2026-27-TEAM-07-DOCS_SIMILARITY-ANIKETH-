package com.docs_similarity.model;

import java.util.List;

/** A page of the Suffix Array and LCP table. */
public record SuffixArrayPage(String analysisId, int page, int pageSize, int totalRows, int totalPages,
                              int minMatchLength, List<SuffixArrayRow> rows) {
}
