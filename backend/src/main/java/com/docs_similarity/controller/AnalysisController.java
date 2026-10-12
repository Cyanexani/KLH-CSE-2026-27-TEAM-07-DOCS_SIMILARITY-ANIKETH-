package com.docs_similarity.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docs_similarity.exception.ApiException;
import com.docs_similarity.model.AnalysisResponse;
import com.docs_similarity.model.AnalyzeRequest;
import com.docs_similarity.model.DocumentInput;
import com.docs_similarity.model.SuffixArrayPage;
import com.docs_similarity.service.DocumentSimilarityService;
import com.docs_similarity.service.DocumentValidator;

/**
 * REST endpoints of the analysis. This class only deals with HTTP: reading the request,
 * delegating to {@link DocumentSimilarityService} and returning JSON.
 */
@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final DocumentSimilarityService service;
    private final DocumentValidator validator;

    public AnalysisController(DocumentSimilarityService service, DocumentValidator validator) {
        this.service = service;
        this.validator = validator;
    }

    /** Analyze documents sent as JSON: {"documents":[{"name":"a.txt","content":"..."}], "minMatchLength":25}. */
    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResponse analyzeJson(@RequestBody AnalyzeRequest request) {
        if (request == null) {
            throw ApiException.badRequest("The request body is empty.");
        }
        return service.analyze(request.documents(), request.minMatchLength());
    }

    /** Analyze uploaded .txt files sent as multipart/form-data (field name "files"). */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyzeFiles(@RequestParam(value = "files", required = false) List<MultipartFile> files,
                                         @RequestParam(value = "minMatchLength", required = false) Integer minMatchLength) {
        List<DocumentInput> documents = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                validator.checkUploadFileName(file.getOriginalFilename());
                try {
                    documents.add(new DocumentInput(file.getOriginalFilename(),
                            new String(file.getBytes(), StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not read uploaded file " + file.getOriginalFilename(), e);
                }
            }
        }
        return service.analyze(documents, minMatchLength);
    }

    /** One page of the Suffix Array and LCP Array of a previous analysis. */
    @GetMapping("/analysis/{analysisId}/suffix-array")
    public SuffixArrayPage suffixArray(@PathVariable String analysisId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "25") int size) {
        return service.suffixArrayPage(analysisId, page, size);
    }
}
