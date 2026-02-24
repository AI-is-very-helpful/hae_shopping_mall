package com.hae.shop.interfaces.member.dto;

/**
 * 회원 가입 요청 DTO
 */
public record RegisterRequest(
    String email,
    String password,
    String nickname
) {
}
