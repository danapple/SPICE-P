package com.danapple.spicep.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

abstract class AbstractApi {
    protected ResponseEntity<String> createErrorResponse(final String errorMessage, final HttpStatus status) {
        return new ResponseEntity<>(errorMessage, status);
    }
}
