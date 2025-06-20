package com.team5.catdogeats.addresses.exception;

public class AddressAccessDeniedException extends RuntimeException {
    public AddressAccessDeniedException(String message) {
        super(message);
    }

    public AddressAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}