package com.team5.catdogeats.chats.repository;

import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessages, String> {
}
