package com.team5.catdogeats.users.exception;

public class WithdrawnAccountDomainException extends RuntimeException {
    public WithdrawnAccountDomainException(String message) {
        super(message);
    }
}
