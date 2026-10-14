package com.docs_similarity.controller;

import com.docs_similarity.extraction.ExtractionException;
import com.docs_similarity.model.AnalysisResponse;
import com.docs_similarity.service.DocumentSimilarityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
public class AnalysisController {

    private final DocumentSimilarityService service;

    public AnalysisController(DocumentSimilarityService service) {
        this.service = service;
    }

    /**
     * POST /api/analyze
     * multipart/form-data, field name "files" holding 2+ documents in any
     * mix of TXT, PDF, and DOCX. Returns the full DSA pipeline result:
     * ranking, common word sequences, summary stats and algorithm/technical details.
     */
    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<?> analyze(@RequestParam("files") List<MultipartFile> files) {
        try {
            AnalysisResponse response = service.analyze(files);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | ExtractionException e) {
            // Both validation errors and extraction failures (unsupported type,
            // corrupt file, scanned/image-only PDF with no text layer, etc.) are
            // reported the same way: a clear 400 with a specific, user-facing message.
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to read uploaded files: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal analysis error: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "DOCS_SIMILARITY"));
    }
}
