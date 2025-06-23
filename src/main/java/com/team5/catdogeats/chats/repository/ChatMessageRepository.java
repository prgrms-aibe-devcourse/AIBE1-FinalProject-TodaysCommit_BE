package com.team5.catdogeats.chats.repository;

import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessages, String> {
    Page<ChatMessages> findByRoomIdOrderBySentAtDesc(String roomId, Pageable pageable);

    List<ChatMessages> findByRoomIdAndIsReadFalseAndSenderIdNot(String roomId, String senderId);

}
