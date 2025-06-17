package com.team5.catdogeats.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomController {
    @GetMapping("/")
    public ResponseEntity<?> home() {
        return ResponseEntity.ok("Welcome to CatDogeats");
    }
}
