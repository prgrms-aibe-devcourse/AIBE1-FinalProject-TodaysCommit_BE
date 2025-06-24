package com.team5.catdogeats.products.service;

/**
 * 만료된 재고 예약 처리 배치 서비스 인터페이스
 */
public interface ExpiredReservationBatchService {

    /**
     * 만료된 예약 처리
     * @return 처리된 예약 개수
     */
    int processExpiredReservations();
}