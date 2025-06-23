package com.team5.catdogeats.chats.repository;

import com.team5.catdogeats.chats.domain.ChatRooms;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatRoomRepository extends MongoRepository<ChatRooms, String> {

    List<ChatRooms> findByIdOrderByCreatedAtDesc(String chatRoomId);
}
