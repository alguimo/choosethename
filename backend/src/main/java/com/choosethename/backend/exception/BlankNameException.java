package com.choosethename.backend.exception;

public class BlankNameException extends RuntimeException {
    public BlankNameException(String message) {
        super(message);
    }
}