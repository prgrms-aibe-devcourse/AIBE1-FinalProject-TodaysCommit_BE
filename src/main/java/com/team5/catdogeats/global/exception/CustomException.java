package com.team5.catdogeats.global.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}