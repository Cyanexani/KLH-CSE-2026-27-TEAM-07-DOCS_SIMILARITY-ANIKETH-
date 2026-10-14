package com.docs_similarity.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * PDF text extraction using Apache PDFBox. Only the text layer already
 * embedded in the PDF is read — no OCR is performed. A PDF whose pages
 * are scanned images with no embedded text layer will extract to an
 * empty/blank string, which the caller (DocumentTextExtractor) turns
 * into a clear, specific user-facing error rather than silently treating
 * the document as analyzed.
 */
public class PdfTextExtractor implements TextExtractor {

    @Override
    public String extractText(byte[] fileBytes, String filename) throws ExtractionException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(fileBytes))) {
            if (document.isEncrypted()) {
                throw new ExtractionException(
                        "'" + filename + "' is a password-protected PDF and cannot be read. " +
                        "Please upload an unprotected copy.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (ExtractionException e) {
            throw e;
        } catch (IOException e) {
            throw new ExtractionException(
                    "'" + filename + "' could not be read as a valid PDF file. It may be corrupt or truncated.", e);
        } catch (Exception e) {
            throw new ExtractionException(
                    "An unexpected error occurred while reading PDF '" + filename + "': " + e.getMessage(), e);
        }
    }
}
