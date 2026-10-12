package com.docs_similarity.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.docs_similarity.config.AppProperties;
import com.docs_similarity.dsa.CombinedText;

/**
 * Keeps the Suffix Array and LCP Array of the most recent analyses in memory, so the
 * frontend can page through them without the whole arrays being sent at once.
 * The oldest analysis is dropped when the capacity is reached.
 */
@Component
public class AnalysisStore {

    /** Data needed to render the Suffix Array and LCP table of one analysis. */
    public record StoredAnalysis(String id, CombinedText text, int[] suffixArray, int[] lcp,
                                 List<String> documentNames, int minMatchLength) {
    }

    private final Map<String, StoredAnalysis> analyses;

    public AnalysisStore(AppProperties properties) {
        final int capacity = Math.max(1, properties.analysis().storedAnalyses());
        this.analyses = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, StoredAnalysis> eldest) {
                return size() > capacity;
            }
        };
    }

    public synchronized void save(StoredAnalysis analysis) {
        analyses.put(analysis.id(), analysis);
    }

    public synchronized Optional<StoredAnalysis> find(String id) {
        return Optional.ofNullable(analyses.get(id));
    }
}
