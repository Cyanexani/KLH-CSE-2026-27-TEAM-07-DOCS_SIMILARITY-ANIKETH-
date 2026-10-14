package com.docs_similarity.controller;

import com.docs_similarity.config.CorsConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/** Reports an upload over the multipart limits as a 413 with the usual {"error": ...} body. */
@RestControllerAdvice
public class UploadLimitExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error",
                "Upload rejected: the limit is " + CorsConfig.MAX_FILE_SIZE.toMegabytes() + " MB per file and "
                        + CorsConfig.MAX_REQUEST_SIZE.toMegabytes() + " MB per request."));
    }
}
