package com.stockmatch.user.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.portfolio.domain.Portfolio;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name ="provider_id", length = 255)
    private String providerId;

    @Column(length = 100)
    private String email;

    @Column(name = "email_verified")
    private boolean emailVerified;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(name = "profile_image_url", length = 255)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    private UserInvestmentType investmentType;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Portfolio portfolio;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AlphaVantageKey alphaVantageKey;

    //OAuth2 신규 사용자 생성을 위한 생성자
    @Builder
    public User(AuthProvider provider, String providerId , String email, String name, String profileImageUrl){
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.emailVerified =  true; //소셜 로그인은 이메일 인증 되었다고 가정
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.lastLoginAt = LocalDateTime.now();
    }

    //이미 존재하는 사용자 로그인시 업데이트
    public User updateOAuthInfo(String name, String profileImageUrl){
        this.name =name;
        this.profileImageUrl = profileImageUrl;
        this.lastLoginAt = LocalDateTime.now();
        return this;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }

}
