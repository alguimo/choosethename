package com.choosethename.backend.exception;

public class StaleVoteException extends RuntimeException {

    public StaleVoteException(String message) {
        super(message);
    }
}