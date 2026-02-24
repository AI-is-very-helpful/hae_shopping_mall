package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.domain.member.model.Member;
import com.hae.shop.domain.member.port.in.MemberService;
import com.hae.shop.domain.member.port.out.MemberRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepositoryPort memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Member register(String email, String password, String nickname) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        Member member = new Member();
        member.setEmail(email);
        member.setPassword(passwordEncoder.encode(password));
        member.setNickname(nickname);
        member.setRole(Member.MemberRole.ROLE_USER);

        return memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
