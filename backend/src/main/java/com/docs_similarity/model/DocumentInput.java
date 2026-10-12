package com.docs_similarity.model;

/** One document sent by the client: a display name and its full text. */
public record DocumentInput(String name, String content) {
}
