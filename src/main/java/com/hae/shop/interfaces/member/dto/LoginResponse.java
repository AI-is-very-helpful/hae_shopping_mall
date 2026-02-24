package com.hae.shop.interfaces.member.dto;

/**
 * 로그인 성공 응답 DTO (JWT 토큰 포함)
 */
public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    String tokenType,
    MemberResponse member
) {
}
