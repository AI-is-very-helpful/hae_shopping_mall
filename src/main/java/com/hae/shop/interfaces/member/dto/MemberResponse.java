package com.hae.shop.interfaces.member.dto;

import java.time.Instant;

/**
 * 회원 응답 DTO (조회 결과)
 */
public record MemberResponse(
    Long id,
    String email,
    String nickname,
    String role,
    Instant createdAt
) {
}
