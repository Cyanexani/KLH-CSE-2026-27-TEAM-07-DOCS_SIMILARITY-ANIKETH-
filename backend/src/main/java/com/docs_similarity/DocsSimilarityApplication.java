package com.docs_similarity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the DOCS_SIMILARITY backend.
 *
 * <p>Layers:
 * <ul>
 *   <li>{@code controller}: REST endpoints (HTTP only, no algorithm code)</li>
 *   <li>{@code service}: validation and orchestration of the analysis pipeline</li>
 *   <li>{@code dsa}: plain Java algorithms (Suffix Array, Kasai LCP, common substrings,
 *       similarity, ranking) with no Spring dependency</li>
 *   <li>{@code model}: JSON request and response records</li>
 *   <li>{@code util}: text preprocessing</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DocsSimilarityApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocsSimilarityApplication.class, args);
    }
}
