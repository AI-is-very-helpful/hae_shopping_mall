package com.hae.shop.interfaces.member.dto;

/**
 * 로그인 요청 DTO
 */
public record LoginRequest(
    String email,
    String password
) {
}
