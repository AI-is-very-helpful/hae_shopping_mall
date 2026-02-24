package com.hae.shop.domain.member.port.in;

import com.hae.shop.domain.member.model.Member;

public interface MemberService {
    Member register(String email, String password, String nickname);
    Member findByEmail(String email);
    Member findById(Long id);
}
