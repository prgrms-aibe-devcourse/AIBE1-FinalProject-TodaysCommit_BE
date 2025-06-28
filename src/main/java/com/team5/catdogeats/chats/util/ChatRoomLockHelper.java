package com.team5.catdogeats.chats.util;

import com.team5.catdogeats.chats.exception.ChatRoomLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomLockHelper {
    private final RedissonClient redisson;

    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 10L;

    /**
     * 채팅방 락을 획득하고 작업을 실행하는 헬퍼 메서드
     */
    public <T> T executeWithLock(String roomId, Supplier<T> task) {
        String lockKey = "chat:room:" + roomId;
        RLock lock = redisson.getLock(lockKey);

        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                throw new ChatRoomLockException("채팅방 락 획득 실패: " + roomId);
            }

            log.debug("채팅방 락 획득 성공: {}", lockKey);
            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChatRoomLockException("채팅방 락 대기 중 인터럽트 발생: " + roomId, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("채팅방 락 해제: {}", lockKey);
            }
        }
    }


    public void executeWithLock(String roomId, Runnable task) {
        executeWithLock(roomId, () -> {
            task.run();
            return null;
        });
    }
}
