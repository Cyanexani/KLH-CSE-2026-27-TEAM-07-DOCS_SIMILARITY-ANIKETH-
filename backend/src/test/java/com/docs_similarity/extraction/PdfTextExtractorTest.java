package com.docs_similarity.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractorTest {

    /** Builds a real, valid PDF file (in memory) containing the given line of text. */
    private byte[] buildPdfWithText(String text) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** Builds a real, valid PDF with a blank page and no text content at all (simulates a scanned/image-only PDF). */
    private byte[] buildBlankPdf() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void extractsTextFromRealPdf() throws Exception {
        byte[] pdfBytes = buildPdfWithText("The quick brown fox jumps over the lazy dog");
        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(pdfBytes, "sample.pdf");
        assertTrue(text.contains("The quick brown fox jumps over the lazy dog"),
                "Expected extracted PDF text to contain the original sentence, got: " + text);
    }

    @Test
    void blankPageProducesEmptyOrBlankText() throws Exception {
        byte[] pdfBytes = buildBlankPdf();
        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(pdfBytes, "scanned.pdf");
        assertTrue(text.isBlank(), "A page with no text content should extract to blank text");
    }

    @Test
    void corruptPdfBytesThrowExtractionException() {
        byte[] garbage = "this is not a real pdf file at all".getBytes();
        PdfTextExtractor extractor = new PdfTextExtractor();
        ExtractionException ex = assertThrows(ExtractionException.class,
                () -> extractor.extractText(garbage, "broken.pdf"));
        assertTrue(ex.getMessage().contains("broken.pdf"));
    }
}
