package com.docs_similarity.exception;

import org.springframework.http.HttpStatus;

/** An error with a readable message and the HTTP status the client should receive. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException payloadTooLarge(String message) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, message);
    }

    public static ApiException unsupportedMediaType(String message) {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }
}
