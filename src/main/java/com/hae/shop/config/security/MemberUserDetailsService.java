package com.hae.shop.config.security;

import com.hae.shop.domain.member.port.in.MemberService;
import com.hae.shop.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Collection;


/**
 * MemberService를 Spring Security의 UserDetailsService로 어댑팅합니다.
 */
@Component
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberService memberService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberService.findByEmail(email);

        GrantedAuthority authority = new SimpleGrantedAuthority(member.getRole().name());
        return new org.springframework.security.core.userdetails.UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(authority);
            }

            @Override
            public String getPassword() {
                return member.getPassword();
            }

            @Override
            public String getUsername() {
                return member.getEmail();
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }
}
