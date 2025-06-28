package com.team5.catdogeats.chats.mongo.repository;

import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessages, String> {
    // 커서가 있을 경우
    List<ChatMessages> findByRoomIdAndSentAtLessThanOrderBySentAtDesc(
            String roomId, Instant cursor, Pageable pageable);

    // 커서가 없을 경우 (최신 메시지부터)
    List<ChatMessages> findByRoomIdOrderBySentAtDesc(
            String roomId, Pageable pageable);

    List<ChatMessages> findByRoomId(String roomId);
}
