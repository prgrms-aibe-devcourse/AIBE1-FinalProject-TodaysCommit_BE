package com.team5.catdogeats.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Login", description = "로그인 후 보이는 화면")
public class HomeController {
    @GetMapping("/")
    @Operation(summary = "메인 화면", description = "로그인 후 메인 화면으로 리다익트가 되면 됩니다")
    public ResponseEntity<?> home() {
        return ResponseEntity.ok("Welcome to CatDogeats");
    }

    @GetMapping("/withdraw")
    @Operation(summary = "탈퇴한 유저 화면", description = "탈퇴한 유저가 다시 로그인 할 경우")
    public ResponseEntity<?> withdraw() {
        return ResponseEntity.ok("You have been withdrawn from CatDogeats");
    }

}
