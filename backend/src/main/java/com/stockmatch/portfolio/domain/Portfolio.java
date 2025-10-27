package com.stockmatch.portfolio.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.risk.domain.RiskAnalysis;
import com.stockmatch.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name = "portfolio")
public class Portfolio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false)
    @Builder.Default
    private Currency baseCurrency = Currency.KRW;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Holding> holdings = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RiskAnalysis> riskAnalyses = new ArrayList<>();

    // === 연관관계 편의 메서드 === //
    public void setUser(User user) {
        this.user = user;
        if (user.getPortfolio() != this) {
            user.setPortfolio(this);
        }
    }

    public void addHolding(Holding holding) {
        holdings.add(holding);
        if (holding.getPortfolio() != this) {
            holding.setPortfolio(this);
        }
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        if (transaction.getPortfolio() != this) {
            transaction.setPortfolio(this);
        }
    }

    public void addRiskAnalysis(RiskAnalysis riskAnalysis) {
        riskAnalyses.add(riskAnalysis);
        if (riskAnalysis.getPortfolio() != this) {
            riskAnalysis.setPortfolio(this);
        }
    }
}
