package com.docs_similarity.model;

/** Body returned for every error, so the frontend can always show a readable message. */
public record ErrorResponse(int status, String error, String message, String path, String timestamp) {
}
