package com.choosethename.backend.exception;

public class ResultsNotReadyException extends RuntimeException {

    public ResultsNotReadyException(String message) {
        super(message);
    }
}