package com.team5.catdogeats.global.exception;

public class ExpiredTokenException extends TokenErrorException {
    public ExpiredTokenException() {
        super("Expired JWT token.");
    }
}
