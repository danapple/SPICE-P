package com.danapple.spicep.api;

public class ValidationException extends Exception {
    public ValidationException(String validationErrorMessage) {
        super(validationErrorMessage);
    }
}
