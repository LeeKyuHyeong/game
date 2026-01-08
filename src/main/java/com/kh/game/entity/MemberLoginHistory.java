package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_login_history")
@Getter
@Setter
@NoArgsConstructor
public class MemberLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoginResult result;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "fail_reason", length = 255)
    private String failReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum LoginResult {
        SUCCESS, FAIL_PASSWORD, FAIL_NOT_FOUND, FAIL_BANNED, FAIL_INACTIVE
    }

    // 성공 로그인 기록 생성
    public static MemberLoginHistory success(Member member, String ipAddress, String userAgent) {
        MemberLoginHistory history = new MemberLoginHistory();
        history.setMember(member);
        history.setEmail(member.getEmail());
        history.setResult(LoginResult.SUCCESS);
        history.setIpAddress(ipAddress);
        history.setUserAgent(userAgent);
        return history;
    }

    // 실패 로그인 기록 생성
    public static MemberLoginHistory fail(String email, LoginResult result, String failReason, String ipAddress, String userAgent) {
        MemberLoginHistory history = new MemberLoginHistory();
        history.setEmail(email);
        history.setResult(result);
        history.setFailReason(failReason);
        history.setIpAddress(ipAddress);
        history.setUserAgent(userAgent);
        return history;
    }
}