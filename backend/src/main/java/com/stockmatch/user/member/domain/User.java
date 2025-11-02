package com.stockmatch.user.member.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.enums.UserInvestmentType;
import com.stockmatch.user.member.domain.enums.UserRole;
import com.stockmatch.user.member.domain.enums.UserStatus;
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

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Portfolio portfolio;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private AlphaVantageKey alphaVantageKey;

    // === 연관관계 편의 메서드 ===  //
    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
        if (portfolio.getUser() != this) {
            portfolio.setUser(this);
        }
    }

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

        if (this.status == UserStatus.DELETED){
            this.status = UserStatus.ACTIVE;
            this.deletedAt = null;
        }
        return this;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }


    public void updateProfile(String name){
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public void deactivate() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
