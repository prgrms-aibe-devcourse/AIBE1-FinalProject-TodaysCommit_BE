package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
public class WithdrawController {
    private final WithdrawService withdrawService;

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<?>> withdraw(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

       try {
           withdrawService.withdraw(userPrincipal);
           return ResponseEntity.ok(ApiResponse.success(ResponseCode.USER_SOFT_DELETE_SUCCESS));
       } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ResponseCode.ACCESS_DENIED));
       } catch (Exception e) {
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
       }
    }
}
