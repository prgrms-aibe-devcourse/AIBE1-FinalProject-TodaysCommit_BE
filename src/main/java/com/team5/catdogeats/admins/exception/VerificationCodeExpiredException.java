package com.team5.catdogeats.admins.exception;

/**
 * 인증코드 만료 예외
 */
public class VerificationCodeExpiredException extends RuntimeException {
    public VerificationCodeExpiredException(String message) {
        super(message);
    }
}
