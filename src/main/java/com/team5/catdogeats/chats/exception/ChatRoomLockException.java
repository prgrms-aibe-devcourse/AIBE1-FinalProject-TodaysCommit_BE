package com.team5.catdogeats.chats.exception;

public class ChatRoomLockException extends RuntimeException {
    public ChatRoomLockException(String message) {
        super(message);
    }
    public ChatRoomLockException(String message, Throwable cause) {
        super(message, cause);
    }

}
