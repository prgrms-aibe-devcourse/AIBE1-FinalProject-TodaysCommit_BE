package com.team5.catdogeats.chats.mongo.repository;

import com.team5.catdogeats.chats.domain.ChatRooms;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRoomRepository extends MongoRepository<ChatRooms, String> {

}
