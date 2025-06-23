package com.team5.catdogeats.products.exception;

public class DuplicateProductNumberException extends RuntimeException {
    public DuplicateProductNumberException(String message) {
        super(message);
    }
}