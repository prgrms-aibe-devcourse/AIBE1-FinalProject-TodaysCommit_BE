package com.team5.catdogeats.users.exception;

/**
 * 사업자 등록번호가 중복될 때 발생하는 예외
 */
public class BusinessNumberDuplicateException extends RuntimeException {
    public BusinessNumberDuplicateException(String message) {
        super(message);
    }
}