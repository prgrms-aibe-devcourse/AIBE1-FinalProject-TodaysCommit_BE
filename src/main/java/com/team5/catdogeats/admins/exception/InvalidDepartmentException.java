package com.team5.catdogeats.admins.exception;

/**
 * 잘못된 부서 예외
 */
public class InvalidDepartmentException extends RuntimeException {
    public InvalidDepartmentException(String message) {
        super(message);
    }
}
