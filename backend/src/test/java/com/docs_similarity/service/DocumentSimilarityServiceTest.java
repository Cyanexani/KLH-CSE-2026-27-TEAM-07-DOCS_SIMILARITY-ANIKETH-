package com.docs_similarity.service;

import com.docs_similarity.model.AnalysisResponse;
import com.docs_similarity.model.DocumentResult;
import com.docs_similarity.model.SimilarityResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentSimilarityServiceTest {

    private final DocumentSimilarityService service = new DocumentSimilarityService();

    private MultipartFile txt(String name, String content) {
        return new MockMultipartFile("files", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    private MultipartFile pdf(String name, String content) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(50, 700);
                // Break long content into lines PDFBox can render without special wrapping logic
                int max = Math.min(content.length(), 90);
                cs.showText(content.substring(0, max));
                cs.endText();
                if (content.length() > max) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 11);
                    cs.newLineAtOffset(50, 680);
                    cs.showText(content.substring(max));
                    cs.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new MockMultipartFile("files", name, "application/pdf", out.toByteArray());
        }
    }

    private MultipartFile docx(String name, String content) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun run = p.createRun();
            run.setText(content);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return new MockMultipartFile("files", name,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
        }
    }

    private MultipartFile blankPdf(String name) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new MockMultipartFile("files", name, "application/pdf", out.toByteArray());
        }
    }

    @Test
    void testIdenticalDocumentsHaveHighSimilarity() throws Exception {
        String content = "The suffix array is a sorted array of all suffixes of a given string, "
                + "and it is often paired with the LCP array for efficient string processing.";

        List<MultipartFile> files = List.of(
                txt("a.txt", content),
                txt("b.txt", content)
        );

        AnalysisResponse response = service.analyze(files);
        SimilarityResult result = response.getRanking().get(0);

        assertTrue(result.getSimilarityPercent() > 95.0,
                "Identical documents should score very high similarity, got " + result.getSimilarityPercent());
    }

    @Test
    void testIdenticalDocumentsScoreExactly100() throws Exception {
        String content = "Machine learning algorithms, as the report explains, improve with more data.";
        AnalysisResponse response = service.analyze(List.of(txt("a.txt", content), txt("b.txt", content)));
        SimilarityResult result = response.getRanking().get(0);

        assertEquals(100.0, result.getSimilarityPercent(), 0.001);
        assertEquals(11, result.getSharedWords());
        assertEquals(1, result.getTotalCommonSequences());
        assertEquals("machine learning algorithms as the report explains improve with more data",
                result.getTopCommonSequences().get(0).getText());
    }

    @Test
    void testCompletelyDifferentDocumentsHaveLowSimilarity() throws Exception {
        List<MultipartFile> files = List.of(
                txt("a.txt", "Stock markets rallied after the central bank announcement on Tuesday."),
                txt("b.txt", "A local football club won the regional championship final yesterday.")
        );

        AnalysisResponse response = service.analyze(files);
        SimilarityResult result = response.getRanking().get(0);

        assertEquals(0.0, result.getSimilarityPercent(), 0.001,
                "Documents without a shared word sequence should score 0% similarity");
        assertTrue(result.getTopCommonSequences().isEmpty());
    }

    @Test
    void testCharacterOverlapWithoutSharedWordsScoresZero() throws Exception {
        // The old character-level engine matched "environment" / "nvironment" / "vironment".
        AnalysisResponse response = service.analyze(List.of(
                txt("a.txt", "environment environmental environments"),
                txt("b.txt", "nvironment vironment ironment")));
        assertEquals(0.0, response.getRanking().get(0).getSimilarityPercent(), 0.001);
    }

    @Test
    void testSimilarityFormulaUsesSharedWordsOverTotalWords() throws Exception {
        // A: 5 words, B: 10 words, shared sequence "alpha beta gamma delta" = 4 words
        // Similarity = 2 x 4 / (5 + 10) x 100 = 53.33
        AnalysisResponse response = service.analyze(List.of(
                txt("a.txt", "Alpha, beta, gamma, delta, epsilon."),
                txt("b.txt", "alpha beta gamma delta zeta eta theta iota kappa lambda")));
        SimilarityResult r = response.getRanking().get(0);

        assertEquals(5, r.getWordCountA());
        assertEquals(10, r.getWordCountB());
        assertEquals(4, r.getSharedWords());
        assertEquals(53.33, r.getSimilarityPercent(), 0.001);
        assertEquals(1, r.getTopCommonSequences().size());
        assertEquals(4, r.getTopCommonSequences().get(0).getWordCount());
        assertEquals(0, r.getTopCommonSequences().get(0).getOffsetInA());
        assertEquals(0, r.getTopCommonSequences().get(0).getOffsetInB());
    }

    @Test
    void testOverlappingMatchesAreNotDoubleCounted() throws Exception {
        // One 7-word shared passage. Its suffixes produce raw LCP matches of 7, 6, 5, 4 and 3
        // words (25 in total); counted once, the passage is 7 shared words.
        String shared = "one two three four five six seven";
        AnalysisResponse response = service.analyze(List.of(
                txt("a.txt", shared + " apples"),
                txt("b.txt", "bananas " + shared)));
        SimilarityResult r = response.getRanking().get(0);

        assertTrue(response.getAlgorithmDetails().getRawSuffixMatches() >= 5,
                "Several overlapping suffix matches exist before merging");
        assertEquals(7, r.getSharedWordsInA());
        assertEquals(7, r.getSharedWordsInB());
        assertEquals(7, r.getSharedWords());
        assertEquals(1, r.getTotalCommonSequences());
        assertEquals(2.0 * 7 / 16 * 100, r.getSimilarityPercent(), 0.01);
    }

    @Test
    void testRepeatedWordsCannotPushSimilarityTo100() throws Exception {
        // "the the the" is shared; A has 5 words, B has 3. Every word of A is inside some
        // 3-word match, but B can only supply 3 shared words: 2 x 3 / 8 = 75%.
        AnalysisResponse response = service.analyze(List.of(
                txt("a.txt", "the the the the the"),
                txt("b.txt", "the the the")));
        SimilarityResult r = response.getRanking().get(0);

        assertEquals(5, r.getSharedWordsInA());
        assertEquals(3, r.getSharedWordsInB());
        assertEquals(75.0, r.getSimilarityPercent(), 0.001);
        assertEquals(1, r.getTotalCommonSequences());
    }

    @Test
    void testDocumentWithOnlyPunctuationIsRejected() {
        List<MultipartFile> files = List.of(
                txt("a.txt", "some real content here for testing"),
                txt("b.txt", "... !!! ---"));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> service.analyze(files));
        assertTrue(ex.getMessage().contains("b.txt"));
    }

    @Test
    void testAlgorithmDetailsAreWordBased() throws Exception {
        AnalysisResponse response = service.analyze(List.of(
                txt("a.txt", "Solar power grows fast."),
                txt("b.txt", "Wind power grows too.")));
        var details = response.getAlgorithmDetails();

        assertEquals(10, details.getCombinedTokenCount(), "4 + 4 words + 2 sentinels");
        assertEquals(10, details.getSuffixArrayTotalEntries());
        assertEquals(DocumentSimilarityService.MIN_SEQUENCE_WORDS, details.getMinSequenceWords());
        assertEquals("⟨END⟩", details.getSuffixArraySample().get(0).getSuffixPreview().split(" ")[0],
                "Sentinels are the smallest tokens and sort first");
        boolean hasWordPreview = details.getSuffixArraySample().stream()
                .anyMatch(e -> e.getSuffixPreview().startsWith("power grows fast"));
        assertTrue(hasWordPreview, "Suffix previews are word sequences");
        for (DocumentResult d : response.getDocuments()) {
            assertEquals(4, d.getWordCount());
        }
    }

    @Test
    void testRankingIsSortedDescending() throws Exception {
        List<MultipartFile> files = List.of(
                txt("a.txt", "the quick brown fox jumps over the lazy dog repeatedly every single morning"),
                txt("b.txt", "the quick brown fox jumps over the lazy dog repeatedly every single morning too"),
                txt("c.txt", "completely unrelated content about database systems and query optimization strategies")
        );

        AnalysisResponse response = service.analyze(files);
        List<SimilarityResult> ranking = response.getRanking();

        for (int i = 1; i < ranking.size(); i++) {
            assertTrue(ranking.get(i - 1).getSimilarityPercent() >= ranking.get(i).getSimilarityPercent(),
                    "Ranking must be sorted from highest to lowest similarity");
            assertEquals(i, ranking.get(i - 1).getRank());
        }
        assertEquals("a.txt", ranking.get(0).getDocAName());
        assertEquals("b.txt", ranking.get(0).getDocBName());
    }

    @Test
    void testRankingOrdersPairsByWordSimilarity() throws Exception {
        String base = "the council approved a new budget for public transport and road repairs";
        List<MultipartFile> files = List.of(
                txt("original.txt", base),
                txt("copy.txt", base),
                txt("partial.txt", "critics said the council approved a new budget without consultation"),
                txt("unrelated.txt", "the orchestra performed a symphony to a sold out audience"));

        List<SimilarityResult> ranking = service.analyze(files).getRanking();

        assertEquals(6, ranking.size());
        assertEquals(100.0, ranking.get(0).getSimilarityPercent(), 0.001);
        assertEquals(java.util.Set.of("original.txt", "copy.txt"),
                java.util.Set.of(ranking.get(0).getDocAName(), ranking.get(0).getDocBName()));
        // The two pairs involving partial.txt share "the council approved a new budget" (6 words)
        for (int i = 1; i <= 2; i++) {
            SimilarityResult r = ranking.get(i);
            assertTrue(r.getDocAName().equals("partial.txt") || r.getDocBName().equals("partial.txt"));
            assertEquals(6, r.getSharedWords());
            assertEquals(r.getSimilarityPercent(),
                    Math.round(DocumentSimilarityService.similarityPercent(6, r.getWordCountA(), r.getWordCountB()) * 100.0) / 100.0,
                    0.0001);
        }
        for (int i = 3; i < 6; i++) {
            assertEquals(0.0, ranking.get(i).getSimilarityPercent(), 0.001, "unrelated.txt shares no 3-word sequence");
            assertEquals(i + 1, ranking.get(i).getRank());
        }
    }

    @Test
    void testRejectsFewerThanTwoDocuments() {
        List<MultipartFile> files = List.of(txt("a.txt", "only one document here"));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> service.analyze(files));
        assertTrue(ex.getMessage().toLowerCase().contains("at least 2"));
    }

    @Test
    void testRejectsEmptyDocument() {
        List<MultipartFile> files = List.of(
                txt("a.txt", "some real content here for testing"),
                txt("b.txt", "")
        );
        assertThrows(IllegalArgumentException.class, () -> service.analyze(files));
    }

    @Test
    void testSummaryCountsMatchActualResults() throws Exception {
        List<MultipartFile> files = List.of(
                txt("a.txt", "alpha beta gamma delta epsilon zeta eta theta"),
                txt("b.txt", "alpha beta gamma delta epsilon zeta eta theta iota kappa"),
                txt("c.txt", "totally different words with no overlap whatsoever present")
        );

        AnalysisResponse response = service.analyze(files);

        assertEquals(3, response.getSummary().getDocumentsAnalyzed());
        assertEquals(response.getRanking().size(), response.getSummary().getPairsCompared());
        assertEquals(3, response.getSummary().getPairsCompared()); // 3 choose 2 = 3
    }

    // ------------------------------------------------------------------
    // Multi-format extraction tests: PDF, DOCX, and mixed-format batches
    // must all enter the exact same DSA pipeline as TXT.
    // ------------------------------------------------------------------

    @Test
    void testIdenticalContentAcrossTxtAndPdfScoresHighSimilarity() throws Exception {
        String content = "Suffix arrays and the Kasai LCP algorithm together enable fast substring "
                + "matching across multiple documents without any external similarity library.";

        List<MultipartFile> files = List.of(
                txt("a.txt", content),
                pdf("b.pdf", content)
        );

        AnalysisResponse response = service.analyze(files);
        SimilarityResult result = response.getRanking().get(0);

        assertTrue(result.getSimilarityPercent() > 80.0,
                "Same textual content in TXT vs PDF form should score high similarity, got "
                        + result.getSimilarityPercent());
    }

    @Test
    void testIdenticalContentAcrossTxtAndDocxScoresHighSimilarity() throws Exception {
        String content = "Apache POI is used here purely to extract plain text from DOCX files "
                + "before handing that text to the unchanged Suffix Array and Kasai LCP pipeline.";

        List<MultipartFile> files = List.of(
                txt("a.txt", content),
                docx("b.docx", content)
        );

        AnalysisResponse response = service.analyze(files);
        SimilarityResult result = response.getRanking().get(0);

        assertTrue(result.getSimilarityPercent() > 80.0,
                "Same textual content in TXT vs DOCX form should score high similarity, got "
                        + result.getSimilarityPercent());
    }

    @Test
    void testMixedTxtPdfDocxBatchAnalyzesTogether() throws Exception {
        String sharedText = "Renewable energy sources such as solar and wind power are becoming "
                + "increasingly important as the world reduces its reliance on fossil fuels.";

        List<MultipartFile> files = List.of(
                txt("doc1.txt", sharedText),
                pdf("doc2.pdf", sharedText),
                docx("doc3.docx", "This document has nothing in common with the others whatsoever, "
                        + "discussing unrelated topics like sailing regattas and marine navigation.")
        );

        AnalysisResponse response = service.analyze(files);

        assertEquals(3, response.getDocuments().size());
        assertEquals(3, response.getSummary().getPairsCompared());

        // The doc1<->doc2 pair (same text, TXT vs PDF) should rank first
        SimilarityResult top = response.getRanking().get(0);
        boolean isTxtPdfPair = (top.getDocAName().equals("doc1.txt") && top.getDocBName().equals("doc2.pdf"))
                || (top.getDocAName().equals("doc2.pdf") && top.getDocBName().equals("doc1.txt"));
        assertTrue(isTxtPdfPair, "Expected the TXT/PDF pair with shared content to rank first, got: "
                + top.getDocAName() + " <-> " + top.getDocBName());

        // Every document result should report its correct source format and a success status
        for (DocumentResult doc : response.getDocuments()) {
            assertEquals("SUCCESS", doc.getExtractionStatus());
            if (doc.getFilename().endsWith(".txt")) assertEquals("TXT", doc.getSourceFormat());
            if (doc.getFilename().endsWith(".pdf")) assertEquals("PDF", doc.getSourceFormat());
            if (doc.getFilename().endsWith(".docx")) assertEquals("DOCX", doc.getSourceFormat());
        }
    }

    @Test
    void testRejectsUnsupportedFileType() {
        List<MultipartFile> files = List.of(
                txt("a.txt", "some real content here for testing purposes"),
                new MockMultipartFile("files", "b.xlsx", "application/vnd.ms-excel", "fake spreadsheet bytes".getBytes())
        );

        Exception ex = assertThrows(Exception.class, () -> service.analyze(files));
        assertTrue(ex.getMessage().contains("b.xlsx"));
        assertTrue(ex.getMessage().contains("TXT"));
        assertTrue(ex.getMessage().contains("PDF"));
        assertTrue(ex.getMessage().contains("DOCX"));
    }

    @Test
    void testRejectsScannedImageOnlyPdfWithClearError() {
        List<MultipartFile> files;
        try {
            files = List.of(
                    txt("a.txt", "some real content here for testing purposes"),
                    blankPdf("scanned.pdf")
            );
        } catch (Exception e) {
            fail("Failed to build test fixture: " + e.getMessage());
            return;
        }

        Exception ex = assertThrows(Exception.class, () -> service.analyze(files));
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("scanned") || msg.contains("image"),
                "Expected a clear scanned/image-only PDF error, got: " + ex.getMessage());
    }

    @Test
    void testRejectsCorruptPdfFile() {
        List<MultipartFile> files = List.of(
                txt("a.txt", "some real content here for testing purposes"),
                new MockMultipartFile("files", "broken.pdf", "application/pdf", "not a real pdf".getBytes())
        );

        Exception ex = assertThrows(Exception.class, () -> service.analyze(files));
        assertTrue(ex.getMessage().contains("broken.pdf"));
    }
}
