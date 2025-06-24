package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.products.repository.StockReservationRepository;
import com.team5.catdogeats.products.service.ExpiredReservationBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiredReservationBatchServiceImpl implements ExpiredReservationBatchService {

    private final StockReservationRepository stockReservationRepository;

    @Override
    @Transactional
    public int processExpiredReservations() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        int expiredCount = stockReservationRepository.bulkExpireReservations(currentTime);

        if (expiredCount > 0) {
            log.info("만료된 예약 일괄 처리 완료: 처리된 개수={}", expiredCount);
        }

        return expiredCount;
    }
}