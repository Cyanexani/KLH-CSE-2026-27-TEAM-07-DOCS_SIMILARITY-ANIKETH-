package com.docs_similarity.model;

/**
 * One step of the analysis pipeline as it actually ran on the server.
 *
 * @param durationMs measured wall clock time of the step
 * @param detail     numbers produced by the step
 */
public record AnalysisStage(String key, String name, String status, double durationMs, String detail) {
}
