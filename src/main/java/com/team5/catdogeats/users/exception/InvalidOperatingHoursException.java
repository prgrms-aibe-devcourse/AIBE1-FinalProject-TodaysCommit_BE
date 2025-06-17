package com.team5.catdogeats.users.exception;

/**
 * 운영시간 관련 예외
 */
public class InvalidOperatingHoursException extends RuntimeException {
    public InvalidOperatingHoursException(String message) {
        super(message);
    }
}
