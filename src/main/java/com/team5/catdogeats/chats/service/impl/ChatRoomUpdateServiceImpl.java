package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.global.annotation.MongoTransactional;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomUpdateServiceImpl implements ChatRoomUpdateService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserIdCacheService userIdCacheService;
    private final RedissonClient redisson;

    // 락 획득 대기 시간 및 임계 시간 설정
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 10L;

    @Override
    @MongoTransactional
    public void updateRoomOnNewMessage(String roomId, String senderId, String message,
                                       BehaviorType behaviorType, Instant sentAt) {
        String lockKey = "chat:room:" + roomId;
        RLock lock = redisson.getLock(lockKey);


        try {
            // 락 획득 시도 (3초 대기, 10초 후 자동 해제)
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException("채팅방 락 획득 실패: " + roomId);
            }
            log.debug("채팅방 락 획득 성공: {}", lockKey);

            // 채팅방 존재 확인
            ChatRooms chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다: " + roomId));

            // 발신자 역할 조회
            String senderRole = userIdCacheService.getCachedRoleByUserId(senderId);

            if (Role.ROLE_BUYER.toString().equals(senderRole)) {
                // 구매자가 보낸 메시지 → 판매자의 안읽은 개수 증가
                chatRoomRepository.updateLastMessageAndIncrementSellerUnread(
                        roomId, message, sentAt, senderId, behaviorType, 1);
                log.debug("구매자 → 판매자 메시지: roomId={}, 판매자 안읽은 개수 +1", roomId);

            } else if (Role.ROLE_SELLER.toString().equals(senderRole)) {
                // 판매자가 보낸 메시지 → 구매자의 안읽은 개수 증가
                chatRoomRepository.updateLastMessageAndIncrementBuyerUnread(
                        roomId, message, sentAt, senderId, behaviorType, 1);
                log.debug("판매자 → 구매자 메시지: roomId={}, 구매자 안읽은 개수 +1", roomId);

            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + senderRole);
            }

        }  catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("채팅방 락 대기 중 인터럽트 발생: roomId={}", roomId, e);
            throw new RuntimeException("채팅방 업데이트 중 인터럽트 발생", e);
        }  catch (Exception e) {
            log.error("채팅방 업데이트 실패: roomId={}, senderId={}", roomId, senderId, e);
            throw e;
        } finally {
            // 락이 현재 스레드가 소유한 경우에만 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("채팅방 락 해제: {}", lockKey);
            }
        }

    }

    @Override
    @MongoTransactional(propagation = Propagation.REQUIRES_NEW)
    public void markMessagesAsRead(String roomId, String userId) {
        String lockKey = "chat:room:" + roomId;
        RLock lock = redisson.getLock(lockKey);

        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException("채팅방 락 획득 실패: " + roomId);
            }
            // 채팅방 존재 확인
            chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다: " + roomId));

            // 사용자 역할 조회
            String userRole = userIdCacheService.getCachedRoleByUserId(userId);
            Instant readAt = Instant.now();

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                // 구매자가 메시지를 읽음
                chatRoomRepository.resetBuyerUnreadCountAndUpdateLastReadAt(roomId, readAt);
                log.debug("구매자 메시지 읽음 처리: roomId={}, userId={} userRole={}", roomId, userId, userRole);

            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                // 판매자가 메시지를 읽음
                chatRoomRepository.resetSellerUnreadCountAndUpdateLastReadAt(roomId, readAt);
                log.debug("판매자 메시지 읽음 처리: roomId={}, userId={}", roomId, userId);

            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("메시지 읽음 처리 락 대기 중 인터럽트 발생: roomId={}", roomId, e);
            throw new RuntimeException("메시지 읽음 처리 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("메시지 읽음 처리 실패: roomId={}, userId={}", roomId, userId, e);
            throw e;
        } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("메시지 읽음 처리 락 해제: {}", lockKey);
                }
            }

    }

    /**
     * 사용자의 특정 채팅방 안읽은 메시지 개수 조회
     */
    @Override
    public int getUnreadCount(String roomId, String userId) {
        try {
            ChatRooms chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다: " + roomId));

            String userRole = userIdCacheService.getCachedRoleByUserId(userId);

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                return chatRoom.getBuyerUnreadCount();
            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                return chatRoom.getSellerUnreadCount();
            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }

        } catch (Exception e) {
            log.error("안읽은 메시지 개수 조회 실패: roomId={}, userId={}", roomId, userId, e);
            return 0;
        }
    }

    /**
     * 사용자의 전체 안읽은 메시지 개수 조회
     */
    @Override
    public int getTotalUnreadCount(String userId) {
        try {
            String userRole = userIdCacheService.getCachedRoleByUserId(userId);

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                return chatRoomRepository.findByBuyerIdOrderByLastMessageAtDesc(userId)
                        .stream()
                        .mapToInt(ChatRooms::getBuyerUnreadCount)
                        .sum();

            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                return chatRoomRepository.findBySellerIdOrderByLastMessageAtDesc(userId)
                        .stream()
                        .mapToInt(ChatRooms::getSellerUnreadCount)
                        .sum();

            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }

        } catch (Exception e) {
            log.error("전체 안읽은 메시지 개수 조회 실패: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 채팅방의 마지막 메시지 정보만 업데이트 (안읽은 개수 변경 없이)
     */
    @Override
    @MongoTransactional
    public void updateLastMessageOnly(String roomId, String message, Instant sentAt,
                                      String senderId, BehaviorType behaviorType) {
        String lockKey = "chat:room:" + roomId;
        RLock lock = redisson.getLock(lockKey);

        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException("채팅방 락 획득 실패: " + roomId);
            }

            log.debug("마지막 메시지 업데이트 락 획득: {}", lockKey);


            chatRoomRepository.updateLastMessage(roomId, message, sentAt, senderId, behaviorType);
            log.debug("마지막 메시지 정보 업데이트: roomId={}", roomId);

        }  catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("마지막 메시지 업데이트 락 대기 중 인터럽트 발생: roomId={}", roomId, e);
            throw new RuntimeException("마지막 메시지 업데이트 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("마지막 메시지 업데이트 실패: roomId={}", roomId, e);
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("마지막 메시지 업데이트 락 해제: {}", lockKey);
            }
        }
    }



}
