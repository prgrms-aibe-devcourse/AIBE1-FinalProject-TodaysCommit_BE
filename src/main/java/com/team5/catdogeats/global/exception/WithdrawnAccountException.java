package com.team5.catdogeats.global.exception;

import org.springframework.security.authentication.AccountStatusException;

public class WithdrawnAccountException extends AccountStatusException {
  public WithdrawnAccountException() {
    super("이미 탈퇴한 유저입니다.");   // 메시지
  }}
