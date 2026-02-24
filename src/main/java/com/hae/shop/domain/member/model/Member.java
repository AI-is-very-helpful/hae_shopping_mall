package com.hae.shop.domain.member.model;

import java.time.Instant;

public class Member {

    private Long id;
    private String email;
    private String password;
    private String nickname;
    private MemberRole role = MemberRole.ROLE_USER;
    private Instant createdAt;
    private Instant updatedAt;

    public enum MemberRole {
        ROLE_USER, ROLE_ADMIN
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public MemberRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setRole(MemberRole role) { this.role = role; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
