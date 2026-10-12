package com.docs_similarity.model;

/**
 * One row of the Suffix Array and LCP table.
 *
 * @param saIndex            r, the row of the Suffix Array
 * @param suffixPosition     SA[r], where the suffix starts in the combined text
 * @param documentId         document that owns the position
 * @param offset             offset of the position inside that document
 * @param separator          true if the suffix starts with a document separator
 * @param lcp                LCP[r], common prefix length with the previous row
 * @param commonPrefix       the first LCP[r] characters of the suffix (shared with row r-1)
 * @param rest               the following characters of the suffix, separators shown as $1, $2, ...
 * @param crossDocumentMatch LCP[r] &gt;= L and rows r-1 and r come from different documents
 */
public record SuffixArrayRow(int saIndex, int suffixPosition, int documentId, String documentName,
                             int offset, boolean separator, int lcp, String commonPrefix, String rest,
                             boolean crossDocumentMatch) {
}
