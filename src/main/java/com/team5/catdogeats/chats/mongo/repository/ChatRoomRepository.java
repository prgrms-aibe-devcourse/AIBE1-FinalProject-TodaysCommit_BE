package com.team5.catdogeats.chats.mongo.repository;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRooms, String> {
    Optional<ChatRooms> findByBuyerIdAndSellerId(String buyerId, String sellerId);
    List<ChatRooms> findByBuyerIdOrderByLastMessageAtDesc(String buyerId);
    List<ChatRooms> findBySellerIdOrderByLastMessageAtDesc(String sellerId);

    Optional<ChatRooms> findByIdAndSellerId(String id, String sellerId);
    Optional<ChatRooms> findByIdAndBuyerId(String id, String buyerId);


    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastMessage': ?1, 'lastMessageAt': ?2, 'lastSenderId': ?3, 'lastBehaviorType': ?4, 'updatedAt': ?2 } }")
    void updateLastMessage(String roomId, String message, Instant sentAt, String senderId, BehaviorType behaviorType);

    // 구매자 안읽은 메시지 개수 초기화
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'buyerUnreadCount': 0 } }")
    void resetBuyerUnreadCount(String roomId);

    // 판매자 안읽은 메시지 개수 초기화
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'sellerUnreadCount': 0 } }")
    void resetSellerUnreadCount(String roomId);

    // 구매자 마지막 읽음 시간 업데이트
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'buyerLastReadAt': ?1 } }")
    void updateBuyerLastReadAt(String roomId, Instant readAt);

    // 판매자 마지막 읽음 시간 업데이트
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'sellerLastReadAt': ?1 } }")
    void updateSellerLastReadAt(String roomId, Instant readAt);


    // 새 메시지 전송 시 한 번에 업데이트 (마지막 메시지 + 안읽은 개수 증가)
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastMessage': ?1, 'lastMessageAt': ?2, 'lastSenderId': ?3, 'lastBehaviorType': ?4, 'updatedAt': ?2 }, '$inc': { 'buyerUnreadCount': ?5 } }")
    void updateLastMessageAndIncrementBuyerUnread(String roomId, String message, Instant sentAt, String senderId, BehaviorType behaviorType, int increment);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastMessage': ?1, 'lastMessageAt': ?2, 'lastSenderId': ?3, 'lastBehaviorType': ?4, 'updatedAt': ?2 }, '$inc': { 'sellerUnreadCount': ?5 } }")
    void updateLastMessageAndIncrementSellerUnread(String roomId, String message, Instant sentAt, String senderId, BehaviorType behaviorType, int increment);
}
