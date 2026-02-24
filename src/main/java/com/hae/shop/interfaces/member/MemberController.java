package com.hae.shop.interfaces.member;

import com.hae.shop.domain.member.port.in.MemberService;
import com.hae.shop.config.security.JwtTokenProvider;
import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.interfaces.member.dto.LoginRequest;
import com.hae.shop.interfaces.member.dto.LoginResponse;
import com.hae.shop.interfaces.member.dto.MemberResponse;
import com.hae.shop.interfaces.member.dto.RegisterRequest;
import com.hae.shop.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        Member member = memberService.register(request.email(), request.password(), request.nickname());
        MemberResponse response = new MemberResponse(
            member.getId(),
            member.getEmail(),
            member.getNickname(),
            member.getRole().name(),
            member.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        Member member = memberService.findById(id);
        MemberResponse response = new MemberResponse(
            member.getId(),
            member.getEmail(),
            member.getNickname(),
            member.getRole().name(),
            member.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Member member = memberService.findByEmail(request.email());
        
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        String accessToken = jwtTokenProvider.generateAccessToken(member.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getEmail());
        long expiresIn = 3600000L;
        
        MemberResponse memberResponse = new MemberResponse(
            member.getId(),
            member.getEmail(),
            member.getNickname(),
            member.getRole().name(),
            member.getCreatedAt()
        );
        
        LoginResponse response = new LoginResponse(accessToken, refreshToken, expiresIn, "Bearer", memberResponse);
        return ResponseEntity.ok(response);
    }
}
