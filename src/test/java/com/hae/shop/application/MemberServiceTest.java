package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.domain.member.model.Member;
import com.hae.shop.domain.member.port.out.MemberRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepositoryPort memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = new Member();
        testMember.setId(1L);
        testMember.setEmail("test@example.com");
        testMember.setPassword("encodedPassword");
        testMember.setNickname("testUser");
        testMember.setRole(Member.MemberRole.ROLE_USER);
    }

    @Test
    @DisplayName("회원가입 성공")
    void register_whenValidInput_shouldCreateMember() {
        // Arrange
        when(memberRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setId(1L);
            return member;
        });

        // Act
        Member result = memberService.register("test@example.com", "password123", "testUser");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getNickname()).isEqualTo("testUser");
        assertThat(result.getRole()).isEqualTo(Member.MemberRole.ROLE_USER);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    void register_whenEmailAlreadyExists_shouldThrowException() {
        // Arrange
        when(memberRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> memberService.register("test@example.com", "password123", "testUser"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_ALREADY_EXISTS));
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("이메일로 회원 조회 성공")
    void findByEmail_whenMemberExists_shouldReturnMember() {
        // Arrange
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testMember));

        // Act
        Member result = memberService.findByEmail("test@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(memberRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("이메일로 회원 조회 실패 - 존재하지 않음")
    void findByEmail_whenMemberNotExists_shouldThrowException() {
        // Arrange
        when(memberRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> memberService.findByEmail("notfound@example.com"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("ID로 회원 조회 성공")
    void findById_whenMemberExists_shouldReturnMember() {
        // Arrange
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

        // Act
        Member result = memberService.findById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(memberRepository).findById(1L);
    }

    @Test
    @DisplayName("ID로 회원 조회 실패 - 존재하지 않음")
    void findById_whenMemberNotExists_shouldThrowException() {
        // Arrange
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> memberService.findById(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("비밀번호 인코딩 검증")
    void register_shouldEncodePassword() {
        // Arrange
        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        memberService.register("test@example.com", "rawPassword", "nickname");

        // Assert
        verify(passwordEncoder).encode("rawPassword");
        verify(memberRepository).save(argThat(member -> 
            member.getPassword().equals("hashedPassword")
        ));
    }
}
