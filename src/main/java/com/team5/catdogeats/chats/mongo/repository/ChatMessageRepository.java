package com.team5.catdogeats.chats.mongo.repository;

import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessages, String> {
    List<ChatMessages> findByRoomIdOrderBySentAtDesc(String roomId, Pageable pageable);
}
