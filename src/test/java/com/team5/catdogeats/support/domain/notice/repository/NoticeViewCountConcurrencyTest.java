package com.team5.catdogeats.support.domain.notice.repository;

import com.team5.catdogeats.support.domain.Notices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("공지사항 조회수 동시성 테스트 (PostgreSQL)")
class NoticeViewCountConcurrencyTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private String testNoticeId;

    @BeforeEach
    @Transactional
    void setUp() {
        // 테스트용 공지사항 생성 (실제 DB에 저장)
        Notices testNotice = Notices.builder()
                .title("동시성 테스트 공지사항 " + System.currentTimeMillis())
                .content("조회수 동시성 테스트용 공지사항입니다.")
                .viewCount(0L)
                .build();

        testNotice = noticeRepository.save(testNotice);
        testNoticeId = testNotice.getId();

        System.out.println("테스트 공지사항 생성됨 - ID: " + testNoticeId);
    }

    @Test
    @DisplayName("조회수 동시성 테스트 - @Modifying 쿼리로 해결 확인")
    void incrementViewCount_ConcurrencyTest() throws InterruptedException {
        // given
        int threadCount = 50; // 동시 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // 초기 조회수 확인
        Long initialViewCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();

        System.out.println("초기 조회수: " + initialViewCount);

        // when: 50개의 스레드가 동시에 조회수 증가
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 각 스레드에서 새로운 트랜잭션으로 실행
                    incrementViewCountInNewTransaction(testNoticeId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("스레드 실행 중 오류: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue(); // 타임아웃 확인

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();

        // 최신 데이터 조회
        Notices updatedNotice = noticeRepository.findById(testNoticeId).orElseThrow();

        // then: 모든 요청이 정확히 반영되어야 함
        Long expectedViewCount = initialViewCount + threadCount;
        Long actualViewCount = updatedNotice.getViewCount();

        System.out.println("=== @Modifying 쿼리 동시성 테스트 결과 ===");
        System.out.println("초기 조회수: " + initialViewCount);
        System.out.println("동시 요청 수: " + threadCount);
        System.out.println("성공한 요청: " + successCount.get());
        System.out.println("실패한 요청: " + errorCount.get());
        System.out.println("최종 조회수: " + actualViewCount);
        System.out.println("예상 조회수: " + expectedViewCount);

        // 검증
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(actualViewCount).isEqualTo(expectedViewCount);
    }

    @Test
    @DisplayName("조회수 동시성 문제 재현 - 기존 방식 (문제 있는 코드)")
    void incrementViewCount_RaceConditionDemo() throws InterruptedException {
        // given
        int threadCount = 30; // 더 적은 수로 테스트
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 초기 조회수 확인
        Long initialViewCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();

        // when: 기존 방식으로 동시 업데이트 (문제가 있는 방식)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 문제가 있는 방식: 조회 -> 증가 -> 저장
                    Notices notice = noticeRepository.findById(testNoticeId).orElseThrow();
                    notice.setViewCount(notice.getViewCount() + 1);
                    noticeRepository.save(notice);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("스레드 실행 중 오류: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        Notices updatedNotice = noticeRepository.findById(testNoticeId).orElseThrow();

        // then: 동시성 문제로 인해 일부 업데이트가 손실됨을 확인
        Long expectedViewCount = initialViewCount + threadCount;
        Long actualViewCount = updatedNotice.getViewCount();

        System.out.println("=== 동시성 문제 재현 결과 ===");
        System.out.println("초기 조회수: " + initialViewCount);
        System.out.println("동시 요청 수: " + threadCount);
        System.out.println("성공한 요청: " + successCount.get());
        System.out.println("실제 조회수: " + actualViewCount);
        System.out.println("예상 조회수: " + expectedViewCount);
        System.out.println("손실된 조회수: " + (expectedViewCount - actualViewCount));

        // 동시성 문제로 인해 일부 업데이트가 손실되었음을 검증
        assertThat(actualViewCount).isLessThan(expectedViewCount);
    }

    // 새로운 트랜잭션에서 조회수 증가 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewCountInNewTransaction(String noticeId) {
        noticeRepository.incrementViewCount(noticeId);
    }
}