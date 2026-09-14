package com.choosethename.backend.exception;

public class ListOperationException extends IllegalArgumentException {
    public ListOperationException(String message) {
        super(message);
    }
}