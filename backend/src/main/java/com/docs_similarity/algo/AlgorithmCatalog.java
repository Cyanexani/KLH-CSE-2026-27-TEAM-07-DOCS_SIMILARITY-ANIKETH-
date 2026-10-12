package com.docs_similarity.algo;

/** Module 1: which query class is answered by which algorithm family, and at what cost. */
public final class AlgorithmCatalog {

    public record Entry(String query, String family, String algorithm, String cost) {
    }

    private static final Entry[] ENTRIES = {
        new Entry("Pattern search", "String algorithms", "Knuth-Morris-Pratt", "O(n + m)"),
        new Entry("Fuzzy matching", "Dynamic programming", "Wagner-Fischer edit distance", "O(nm)"),
        new Entry("Document similarity", "Suffix structures", "Suffix array + Kasai LCP", "O(n log n)"),
        new Entry("Source assignment", "Network flow", "Edmonds-Karp bipartite matching", "O(V E^2)"),
        new Entry("Graph covering", "NP-hard / approximation", "Matching-based 2-approx vertex cover", "O(E), ratio 2"),
        new Entry("Primality testing", "Randomised algorithms", "Miller-Rabin", "O(k log^3 n)"),
    };

    private AlgorithmCatalog() {
    }

    public static Entry[] entries() {
        return ENTRIES.clone();
    }
}
