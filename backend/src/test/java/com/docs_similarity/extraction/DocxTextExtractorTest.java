package com.docs_similarity.extraction;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DocxTextExtractorTest {

    private byte[] buildDocxWithParagraphs(String... paragraphs) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            for (String p : paragraphs) {
                XWPFParagraph paragraph = doc.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(p);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void extractsTextFromRealDocx() throws Exception {
        byte[] docxBytes = buildDocxWithParagraphs(
                "The quick brown fox jumps over the lazy dog.",
                "This is a second paragraph with more content for testing."
        );
        DocxTextExtractor extractor = new DocxTextExtractor();
        String text = extractor.extractText(docxBytes, "sample.docx");

        assertTrue(text.contains("The quick brown fox jumps over the lazy dog."));
        assertTrue(text.contains("This is a second paragraph with more content for testing."));
    }

    @Test
    void corruptDocxBytesThrowExtractionException() {
        byte[] garbage = "this is definitely not a real docx file".getBytes();
        DocxTextExtractor extractor = new DocxTextExtractor();
        ExtractionException ex = assertThrows(ExtractionException.class,
                () -> extractor.extractText(garbage, "broken.docx"));
        assertTrue(ex.getMessage().contains("broken.docx"));
    }

    @Test
    void emptyDocxProducesBlankOrEmptyText() throws Exception {
        byte[] docxBytes = buildDocxWithParagraphs(); // no paragraphs at all
        DocxTextExtractor extractor = new DocxTextExtractor();
        String text = extractor.extractText(docxBytes, "empty.docx");
        assertTrue(text.isBlank());
    }
}
