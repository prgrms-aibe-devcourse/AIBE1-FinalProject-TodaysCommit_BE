package com.team5.catdogeats.chats.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.*;
import com.team5.catdogeats.chats.service.ChatRoomCreateService;
import com.team5.catdogeats.chats.service.ChatRoomListService;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users/chat")
public class ChatRoomController {

    private final ChatRoomCreateService ChatRoomCreateService;
    private final ChatRoomListService chatRoomListService;

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponseDTO>> createRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChatRoomRequestDTO requestDTO) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        try {
            ChatRooms room = ChatRoomCreateService.createRoom(userPrincipal, requestDTO.vendorName());
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, new ChatRoomResponseDTO(room.getId(), room.getCreatedAt(), room.getUpdatedAt())));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ResponseCode.ACCESS_DENIED));
        } catch (Exception e) {
            log.error("Error creating chat room", e);
            return ResponseEntity.badRequest().build();
        }

    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomPageResponseDTO<ChatRoomListDTO>>> getChatHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer size) {

        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        try {
            ChatRoomPageRequestDTO pageRequest = ChatRoomPageRequestDTO.builder()
                    .cursor(cursor)
                    .size(size)
                    .build();

            ChatRoomPageResponseDTO<ChatRoomListDTO> response =
                    chatRoomListService.getChatRooms(principal, pageRequest);


            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        }  catch (NoSuchElementException e) {
           return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND));
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ResponseCode.ACCESS_DENIED));
        } catch (Exception e) {
           log.error("Error getting chat history", e);
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
       }


    }
}
