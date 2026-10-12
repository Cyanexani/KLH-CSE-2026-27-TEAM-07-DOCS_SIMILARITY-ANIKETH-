package com.docs_similarity.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under the {@code docs-similarity} prefix in application.properties.
 * Every value has a default, so the application also starts without the properties file.
 */
@ConfigurationProperties(prefix = "docs-similarity")
public record AppProperties(@DefaultValue Cors cors,
                            @DefaultValue Limits limits,
                            @DefaultValue Analysis analysis,
                            @DefaultValue("../sample-documents") String samplesDir) {

    /** Origins allowed to call the API from a browser (the Vite dev server by default). */
    public record Cors(@DefaultValue({"http://localhost:5173", "http://127.0.0.1:5173"}) List<String> allowedOrigins) {
    }

    /** Input size limits. */
    public record Limits(@DefaultValue("20") int maxDocuments,
                         @DefaultValue("500000") int maxDocumentChars,
                         @DefaultValue("2000000") int maxTotalChars) {
    }

    /** Analysis parameters. */
    public record Analysis(@DefaultValue("25") int defaultMinMatchLength,
                           @DefaultValue("3") int minAllowedMatchLength,
                           @DefaultValue("1000") int maxAllowedMatchLength,
                           @DefaultValue("50") int maxSegmentsPerPair,
                           @DefaultValue("400") int segmentPreviewChars,
                           @DefaultValue("5") int storedAnalyses) {
    }
}
