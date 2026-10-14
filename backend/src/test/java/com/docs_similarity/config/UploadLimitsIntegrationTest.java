package com.docs_similarity.config;

import jakarta.servlet.MultipartConfigElement;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uploads real multipart requests to the embedded Tomcat server, so the
 * servlet container's multipart limits are exercised (MockMvc would skip them).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadLimitsIntegrationTest {

    private static final long MB = 1024L * 1024L;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private MultipartConfigElement multipartConfig;

    @Test
    void multipartLimitsAre25MbPerFileAnd100MbPerRequest() {
        assertEquals(25 * MB, multipartConfig.getMaxFileSize());
        assertEquals(100 * MB, multipartConfig.getMaxRequestSize());
    }

    @Test
    void a16MbPdfUploadsAndIsAnalyzed() throws Exception {
        String sentence = "Machine learning algorithms help newsrooms sort thousands of articles every day.";
        byte[] pdf = largePdf(sentence);
        assertTrue(pdf.length > 15.9 * MB, "fixture should be about 16 MB, was " + pdf.length);

        ResponseEntity<Map> response = post(file("large.pdf", pdf), file("small.txt", bytes(sentence)));

        assertEquals(HttpStatus.OK, response.getStatusCode(), String.valueOf(response.getBody()));
        assertNotNull(response.getBody().get("ranking"));
    }

    @Test
    void aFileOver25MbIsRejectedWith413() {
        byte[] tooBig = new byte[(int) (26 * MB)];
        Arrays.fill(tooBig, (byte) 'a');

        ResponseEntity<Map> response = post(file("huge.txt", tooBig), file("small.txt", bytes("hello world again")));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertTrue(String.valueOf(response.getBody().get("error")).contains("25 MB"));
    }

    private ResponseEntity<Map> post(ByteArrayResource... files) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (ByteArrayResource f : files) body.add("files", f);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.postForEntity("/api/analyze", new HttpEntity<>(body, headers), Map.class);
    }

    private static ByteArrayResource file(String name, byte[] content) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** A one-page PDF with a text line and a large, incompressible image (about 16 MB). */
    private static byte[] largePdf(String text) throws Exception {
        int side = 2360;
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(1);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                image.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDImageXObject xObject = LosslessFactory.createFromImage(doc, image);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(xObject, 50, 100, 200, 200);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}
