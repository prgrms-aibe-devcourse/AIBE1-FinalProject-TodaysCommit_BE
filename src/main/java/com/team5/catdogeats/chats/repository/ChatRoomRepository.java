package com.team5.catdogeats.chats.repository;

import com.team5.catdogeats.chats.domain.ChatRooms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRooms, String> {

    List<ChatRooms> findByIdOrderByCreatedAtDesc(String chatRoomId);
}
