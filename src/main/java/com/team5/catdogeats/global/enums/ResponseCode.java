package com.team5.catdogeats.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    // === 공통 성공 응답 ===
    SUCCESS(HttpStatus.OK, "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "리소스가 성공적으로 생성되었습니다."),

    // === 공통 오류 응답 ===
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "요청 값의 타입이 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "엑세스 토큰이 만료되지 않았습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "요청을 기다리다 서버에서 타임아웃하였습니다."),

    // === 사용자 관련 오류 ===
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    SELLER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "판매자 권한이 필요합니다."),
    BUYER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "구매자 권한이 필요합니다."),

    // === 판매자 관련 응답 ===
    SELLER_INFO_SUCCESS(HttpStatus.OK, "판매자 정보 조회 성공"),
    SELLER_INFO_SAVE_SUCCESS(HttpStatus.OK, "판매자 정보 저장 성공"),
    SELLER_INFO_NOT_FOUND(HttpStatus.OK, "등록된 판매자 정보가 없습니다."),
    BUSINESS_NUMBER_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 사업자 등록번호입니다."),
    INVALID_OPERATING_HOURS(HttpStatus.BAD_REQUEST, "운영 시간 설정이 올바르지 않습니다."),
    INVALID_CLOSED_DAYS(HttpStatus.BAD_REQUEST, "휴무일 설정이 올바르지 않습니다."),

    // === 상품 관련 응답 ===
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."),
    PRODUCT_SAVE_SUCCESS(HttpStatus.OK, "상품이 성공적으로 저장되었습니다."),
    PRODUCT_DELETE_SUCCESS(HttpStatus.OK, "상품이 성공적으로 삭제되었습니다."),

    // === 주문 관련 응답 ===
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 주문을 찾을 수 없습니다."),
    ORDER_SUCCESS(HttpStatus.OK, "주문이 성공적으로 처리되었습니다."),

    // === 리뷰 관련 응답 ===
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리뷰를 찾을 수 없습니다."),
    REVIEW_SAVE_SUCCESS(HttpStatus.OK, "리뷰가 성공적으로 저장되었습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 리뷰를 작성했습니다."),

    // === 채팅 관련 응답 ===
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_MESSAGE_SUCCESS(HttpStatus.OK, "메시지가 성공적으로 전송되었습니다."),

    // === 알림 관련 응답 ===
    NOTIFICATION_SUCCESS(HttpStatus.OK, "알림이 성공적으로 전송되었습니다."),
    NOTIFICATION_READ_SUCCESS(HttpStatus.OK, "알림을 읽음 처리했습니다."),

    // 추가적인 도메인별 에러 코드 정의 가능
    ;

    private final HttpStatus status;
    private final String message;
}