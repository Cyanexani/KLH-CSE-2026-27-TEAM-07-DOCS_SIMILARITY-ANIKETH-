package com.docs_similarity.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docs_similarity.exception.ApiException;
import com.docs_similarity.model.DemoRequest;
import com.docs_similarity.model.DemoResponse;
import com.docs_similarity.model.DocumentInput;
import com.docs_similarity.service.DocumentSimilarityService;
import com.docs_similarity.service.SampleDocumentService;

/** Supporting endpoints: health check, sample documents and the Suffix Array playground. */
@RestController
@RequestMapping("/api")
public class ToolsController {

    private final DocumentSimilarityService service;
    private final SampleDocumentService samples;

    public ToolsController(DocumentSimilarityService service, SampleDocumentService samples) {
        this.service = service;
        this.samples = samples;
    }

    /** Lets the frontend show whether the backend is reachable. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "DOCS_SIMILARITY backend");
    }

    /** The .txt files of the sample-documents folder, with their full text. */
    @GetMapping("/samples")
    public List<DocumentInput> samples() {
        return samples.loadSamples();
    }

    /** Suffix Array and LCP Array of a short string such as "banana". */
    @PostMapping("/suffix-array/demo")
    public DemoResponse demo(@RequestBody DemoRequest request) {
        if (request == null) {
            throw ApiException.badRequest("The request body is empty.");
        }
        return service.demo(request.text());
    }
}
