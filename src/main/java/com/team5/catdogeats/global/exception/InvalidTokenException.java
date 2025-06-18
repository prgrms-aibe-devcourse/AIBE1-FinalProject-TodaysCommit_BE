package com.team5.catdogeats.global.exception;

public class InvalidTokenException extends TokenErrorException {
    public InvalidTokenException() {
        super("Invalid JWT token.");
    }
}

