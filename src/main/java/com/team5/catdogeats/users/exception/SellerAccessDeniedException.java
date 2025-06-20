package com.team5.catdogeats.users.exception;

/**
 * 판매자 권한 관련 접근 거부 예외
 * Spring Security의 AccessDeniedException과 구분하기 위해 이름 변경
 */
public class SellerAccessDeniedException extends RuntimeException {
    public SellerAccessDeniedException(String message) {
        super(message);
    }
}
