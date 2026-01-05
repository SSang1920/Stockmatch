package com.stockmatch.user.member.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.user.member.domain.enums.UserInvestmentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_investment_profile")
public class UserInvestmentProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 분석 결과
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserInvestmentType investmentType;

    // 총점
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    // 사용자 답변 내역
    @Column(name = "raw_answers", columnDefinition = "TEXT")
    private String rawAnswers;

    @Builder
    public UserInvestmentProfile(User user, UserInvestmentType investmentType, Integer totalScore, String rawAnswers){
        this.user = user;
        this.investmentType = investmentType;
        this.totalScore = totalScore;
        this.rawAnswers = rawAnswers;
    }

    public void updateProfile(UserInvestmentType investmentType, Integer totalScore, String rawAnswers){
        this.investmentType = investmentType;
        this.totalScore = totalScore;
        this.rawAnswers = rawAnswers;
    }
}
