package com.team5.catdogeats.chats.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatRoomRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomResponseDTO;
import com.team5.catdogeats.chats.service.ChatMessageService;
import com.team5.catdogeats.chats.service.ChatRoomService;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users/chat")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponseDTO>> createRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChatRoomRequestDTO requestDTO) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        try {
            ChatRooms room = chatRoomService.CreateRoom(userPrincipal, requestDTO.vendorName());
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
//
//    @GetMapping("/{roomId}/messages")
//    public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> getChatHistory(
//            @AuthenticationPrincipal UserPrincipal principal,
//            @PathVariable String roomId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//
//        if (principal == null) {
//            return ResponseEntity
//                    .status(401)
//                    .body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
//        }
//
//       try {
//           List<ChatMessageDTO> response = chatMessageService
//                   .getRecentMessages(roomId, page, size)
//                   .stream()
//                   .map(ChatMessageDTO::from)
//                   .collect(Collectors.toList());
//           return ResponseEntity.ok(
//                   ApiResponse.success(ResponseCode.SUCCESS, response)
//           );
//       } catch (NoSuchElementException e) {
//           return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND));
//       } catch (Exception e) {
//           log.error("Error getting chat history", e);
//           return ResponseEntity.badRequest().build();
//       }
//

//    }
}
