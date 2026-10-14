package com.docs_similarity.extraction;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TxtTextExtractorTest {

    @Test
    void extractsPlainTextVerbatim() throws Exception {
        TxtTextExtractor extractor = new TxtTextExtractor();
        String content = "Hello world.\nSecond line with unicode: café.";
        String result = extractor.extractText(content.getBytes(StandardCharsets.UTF_8), "note.txt");
        assertEquals(content, result);
    }
}
