package com.docs_similarity.extraction;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;

/**
 * DOCX (Office Open XML) text extraction using Apache POI's XWPF model.
 * Only modern .docx files are supported. Legacy binary .doc files (OLE2
 * format) are NOT supported in this version — POI will fail to open them
 * as an XWPFDocument, which is caught here and reported as a clear error
 * rather than attempted with a different (unimplemented) code path.
 */
public class DocxTextExtractor implements TextExtractor {

    @Override
    public String extractText(byte[] fileBytes, String filename) throws ExtractionException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception e) {
            throw new ExtractionException(
                    "'" + filename + "' could not be read as a valid DOCX file. It may be corrupt, " +
                    "password-protected, or saved in the older .doc format, which is not supported in this version.", e);
        }
    }
}
