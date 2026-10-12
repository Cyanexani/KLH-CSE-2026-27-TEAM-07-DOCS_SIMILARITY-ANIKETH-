package com.docs_similarity.model;

import java.util.List;

/**
 * JSON body of {@code POST /api/analyze}.
 *
 * @param documents      the documents to compare (at least 2)
 * @param minMatchLength optional minimum length L of a common segment, in characters
 */
public record AnalyzeRequest(List<DocumentInput> documents, Integer minMatchLength) {
}
