package com.team5.catdogeats.global.exception;

public class TokenErrorException extends RuntimeException {
    public TokenErrorException(String message) {
        super(message);
    }

    public TokenErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}

