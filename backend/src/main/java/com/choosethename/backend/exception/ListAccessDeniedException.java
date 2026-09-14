package com.choosethename.backend.exception;

public class ListAccessDeniedException extends RuntimeException {
    public ListAccessDeniedException(String message) {
        super(message);
    }
}