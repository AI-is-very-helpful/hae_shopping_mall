package com.hae.shop.domain.member.port.out;

import com.hae.shop.domain.member.model.Member;
import java.util.Optional;

public interface MemberRepositoryPort {
    Optional<Member> findByEmail(String email);
    Optional<Member> findById(Long id);
    Member save(Member member);
    boolean existsByEmail(String email);
}
