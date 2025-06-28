package com.team5.catdogeats.chats.mongo.repository;

import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessages, String> {
}
