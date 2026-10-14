package com.docs_similarity.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTextExtractorTest {

    private final DocumentTextExtractor extractor = new DocumentTextExtractor();

    @Test
    void routesTxtToTxtExtractor() throws Exception {
        String text = extractor.extract("hello world".getBytes(StandardCharsets.UTF_8), "notes.txt");
        assertEquals("hello world", text);
        assertEquals("TXT", extractor.formatOf("notes.txt"));
    }

    @Test
    void routesPdfToPdfExtractor() throws Exception {
        byte[] pdf = buildPdfWithText("extracted pdf content for routing test");
        String text = extractor.extract(pdf, "doc.pdf");
        assertTrue(text.contains("extracted pdf content for routing test"));
        assertEquals("PDF", extractor.formatOf("doc.pdf"));
    }

    @Test
    void routesDocxToDocxExtractor() throws Exception {
        byte[] docx = buildDocxWithText("extracted docx content for routing test");
        String text = extractor.extract(docx, "doc.docx");
        assertTrue(text.contains("extracted docx content for routing test"));
        assertEquals("DOCX", extractor.formatOf("doc.docx"));
    }

    @Test
    void rejectsUnsupportedExtensionWithClearMessage() {
        ExtractionException ex = assertThrows(ExtractionException.class,
                () -> extractor.extract("data".getBytes(), "spreadsheet.xlsx"));
        assertTrue(ex.getMessage().contains("spreadsheet.xlsx"));
        assertTrue(ex.getMessage().contains("TXT"));
        assertTrue(ex.getMessage().contains("PDF"));
        assertTrue(ex.getMessage().contains("DOCX"));
    }

    @Test
    void rejectsLegacyDocExtensionAsUnsupported() {
        ExtractionException ex = assertThrows(ExtractionException.class,
                () -> extractor.extract("data".getBytes(), "old.doc"));
        assertTrue(ex.getMessage().contains("old.doc"));
    }

    @Test
    void scannedImageOnlyPdfProducesSpecificError() throws Exception {
        byte[] blankPdf = buildBlankPdf();
        ExtractionException ex = assertThrows(ExtractionException.class,
                () -> extractor.extract(blankPdf, "scanned.pdf"));
        assertTrue(ex.getMessage().toLowerCase().contains("scanned") || ex.getMessage().toLowerCase().contains("image"),
                "Expected a scanned/image-only specific message, got: " + ex.getMessage());
    }

    @Test
    void emptyTxtProducesGenericEmptyError() {
        ExtractionException ex = assertThrows(ExtractionException.class,
                () -> extractor.extract("   \n\n  ".getBytes(StandardCharsets.UTF_8), "blank.txt"));
        assertTrue(ex.getMessage().contains("blank.txt"));
    }

    // ---- helpers ----

    private byte[] buildPdfWithText(String text) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildBlankPdf() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildDocxWithText(String text) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun run = p.createRun();
            run.setText(text);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }
}
