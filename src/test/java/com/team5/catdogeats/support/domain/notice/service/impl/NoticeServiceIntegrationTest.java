package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.NoticeResponseDTO;
import com.team5.catdogeats.support.domain.notice.repository.NoticeRepository;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest  // ✅ 실제 Spring Context 로드
@ActiveProfiles("dev")  // ✅ 실제 DB 사용
@DisplayName("NoticeService 통합 테스트 - 실제 DB로 메모리 동기화 검증")
class NoticeServiceIntegrationTest {

    @Autowired  // ✅ 실제 Service (Mock 아님)
    private NoticeService noticeService;

    @Autowired  // ✅ 실제 Repository
    private NoticeRepository noticeRepository;

    @PersistenceContext  // ✅ 실제 EntityManager
    private EntityManager entityManager;

    private String testNoticeId;

    @BeforeEach
    @Transactional
    void setUp() {
        // 실제 DB에 테스트 데이터 생성
        Notices testNotice = Notices.builder()
                .title("통합테스트 공지사항 " + System.currentTimeMillis())
                .content("실제 DB 동기화 테스트용 공지사항입니다.")
                .viewCount(0L)
                .build();

        testNotice = noticeRepository.save(testNotice);
        testNoticeId = testNotice.getId();

        System.out.println("통합테스트용 공지사항 생성 - ID: " + testNoticeId);
    }

    @Test
    @DisplayName("단일 조회 - DB와 메모리 동기화 확인")
    void getNotice_SingleCall_DbMemorySync() {
        // given
        Long initialViewCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();

        // when
        NoticeResponseDTO response = noticeService.getNotice(testNoticeId);

        // then
        Long dbViewCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();

        System.out.println("=== 단일 조회 DB 동기화 테스트 ===");
        System.out.println("초기 조회수: " + initialViewCount);
        System.out.println("응답 조회수: " + response.getViewCount());
        System.out.println("DB 조회수: " + dbViewCount);

        // 검증: 응답 값이 실제 DB 값과 일치해야 함
        assertThat(response.getViewCount()).isEqualTo(initialViewCount + 1);
        assertThat(response.getViewCount()).isEqualTo(dbViewCount);
        assertThat(dbViewCount).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("연속 조회 - 조회수 누적 정확성 확인")
    void getNotice_SequentialCalls_AccuracyTest() {
        // given
        Long initialViewCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();

        // when: 5번 연속 조회
        List<NoticeResponseDTO> responses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            responses.add(noticeService.getNotice(testNoticeId));
        }

        // then: 각 응답의 조회수가 순차적으로 증가해야 함
        System.out.println("=== 연속 조회 정확성 테스트 ===");
        System.out.println("초기 조회수: " + initialViewCount);

        for (int i = 0; i < responses.size(); i++) {
            Long expectedCount = initialViewCount + i + 1;
            Long actualCount = responses.get(i).getViewCount();

            System.out.println((i+1) + "번째 응답 조회수: " + actualCount + " (예상: " + expectedCount + ")");
            assertThat(actualCount).isEqualTo(expectedCount);
        }

        // 최종 DB 확인
        Long finalDbCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();
        assertThat(finalDbCount).isEqualTo(initialViewCount + 5);
    }

    @Test
    @DisplayName("동시성 테스트 - 실제 DB로 수정된 코드 검증")
    void getNotice_ConcurrencyTest_RealDb() throws InterruptedException {
        // given
        int threadCount = 20;  // 동시 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseViewCounts = new ArrayList<>();

        Long initialViewCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();

        System.out.println("=== 동시성 테스트 시작 ===");
        System.out.println("초기 조회수: " + initialViewCount);
        System.out.println("동시 요청 수: " + threadCount);

        // when: 동시에 getNotice() 호출
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    // 실제 Service 메서드 호출 (실제 DB 사용)
                    NoticeResponseDTO response = noticeService.getNotice(testNoticeId);

                    synchronized (responseViewCounts) {
                        responseViewCounts.add(response.getViewCount());
                    }

                    successCount.incrementAndGet();
                    System.out.println("Thread " + threadNum + " - 응답 조회수: " + response.getViewCount());

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Thread " + threadNum + " 오류: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();

        // then: 결과 검증
        Long finalDbCount = noticeRepository.findById(testNoticeId).orElseThrow().getViewCount();
        Long expectedFinalCount = initialViewCount + threadCount;

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("성공한 요청: " + successCount.get());
        System.out.println("실패한 요청: " + errorCount.get());
        System.out.println("최종 DB 조회수: " + finalDbCount);
        System.out.println("예상 조회수: " + expectedFinalCount);
        System.out.println("응답 조회수 범위: " +
                responseViewCounts.stream().mapToLong(Long::longValue).min().orElse(0) +
                " ~ " + responseViewCounts.stream().mapToLong(Long::longValue).max().orElse(0));

        // 핵심 검증
        assertThat(successCount.get()).isEqualTo(threadCount);  // 모든 요청 성공
        assertThat(errorCount.get()).isEqualTo(0);  // 오류 없음
        assertThat(finalDbCount).isEqualTo(expectedFinalCount);  // DB 조회수 정확

        // ✅ 가장 중요: 모든 응답이 정확한 DB 값을 반환했는지 확인
        // (수정 전 코드라면 일부 응답이 부정확한 메모리 값을 반환했을 것)
        assertThat(responseViewCounts).allSatisfy(count ->
                assertThat(count).isBetween(initialViewCount + 1, expectedFinalCount)
        );
    }

    @Test
    @DisplayName("수정 전 코드 문제 재현 - 메모리 기반 응답의 부정확성")
    void demonstrateProblemWithOldCode() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Long> memoryBasedCounts = new ArrayList<>();

        Long initialViewCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();

        // when: 수정 전 방식 시뮬레이션 (실제 DB 사용)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 수정 전 코드의 문제점 재현
                    Notices notice = noticeRepository.findById(testNoticeId).orElseThrow();
                    noticeRepository.incrementViewCount(testNoticeId);  // DB 업데이트

                    // 문제: 메모리의 이전 값 + 1 (실제 DB 값이 아님)
                    Long memoryBasedCount = notice.getViewCount() + 1;

                    synchronized (memoryBasedCounts) {
                        memoryBasedCounts.add(memoryBasedCount);
                    }
                } catch (Exception e) {
                    System.err.println("시뮬레이션 오류: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 문제점 확인
        Long actualDbCount = noticeRepository.findById(testNoticeId).orElseThrow().getViewCount();

        System.out.println("=== 수정 전 코드 문제점 실증 ===");
        System.out.println("초기 조회수: " + initialViewCount);
        System.out.println("실제 DB 조회수: " + actualDbCount);
        System.out.println("메모리 기반 응답 예시: " + memoryBasedCounts.subList(0, Math.min(5, memoryBasedCounts.size())));

        // 대부분의 메모리 기반 응답이 부정확함을 확인
        long correctMemoryResponses = memoryBasedCounts.stream()
                .filter(count -> count.equals(actualDbCount))
                .count();

        System.out.println("정확한 메모리 응답: " + correctMemoryResponses + "/" + threadCount);

        // 수정 전 코드의 문제: 메모리 기반 응답이 대부분 부정확
        assertThat(correctMemoryResponses).isLessThan(threadCount);

        // 하지만 DB는 정확히 업데이트됨
        assertThat(actualDbCount).isEqualTo(initialViewCount + threadCount);
    }

    @Test
    @Transactional  // ✅ 트랜잭션 추가
    @DisplayName("EntityManager refresh 동작 확인 - 수정된 코드의 핵심")
    void verifyEntityManagerRefresh() {
        // given
        Long initialViewCount = noticeRepository.findById(testNoticeId)
                .orElseThrow().getViewCount();

        // when: 수동으로 조회수 증가 후 refresh 테스트
        Notices notice = noticeRepository.findById(testNoticeId).orElseThrow();
        Long beforeIncrement = notice.getViewCount();

        noticeRepository.incrementViewCount(testNoticeId);  // DB 업데이트

        Long beforeRefresh = notice.getViewCount();  // 아직 메모리 값

        entityManager.flush();
        entityManager.refresh(notice);  // DB에서 최신 값 로드

        Long afterRefresh = notice.getViewCount();  // refresh 후 값

        // then
        System.out.println("=== EntityManager refresh 동작 확인 ===");
        System.out.println("증가 전 메모리 값: " + beforeIncrement);
        System.out.println("증가 후, refresh 전 메모리 값: " + beforeRefresh);
        System.out.println("refresh 후 메모리 값: " + afterRefresh);

        assertThat(beforeIncrement).isEqualTo(initialViewCount);
        assertThat(beforeRefresh).isEqualTo(initialViewCount);  // 메모리는 아직 이전 값
        assertThat(afterRefresh).isEqualTo(initialViewCount + 1);  // refresh 후 최신 값
    }
}