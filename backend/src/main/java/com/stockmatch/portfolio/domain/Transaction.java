package com.stockmatch.portfolio.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.securityasset.domain.Security;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transaction")
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id")
    private Security security;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(precision = 28, scale = 8, nullable = false)
    private BigDecimal quantity;

    @Column(precision = 28, scale = 8, nullable = false)
    private BigDecimal price;

    @Column(name = "trade_at", nullable = false)
    private LocalDateTime tradeAt;

    @Column(length = 255)
    private String memo;

    // === 연관관계 편의 메서드 === //
    public void setPortfolio(Portfolio newPortfolio) {
        if (this.portfolio == newPortfolio) {
            return;
        }

        // 기존 연관관계 끊기
        if (this.portfolio != null) {
            this.portfolio.getTransactions().remove(this);
        }

        // 새 연관관계 설정
        this.portfolio = newPortfolio;
        if (newPortfolio != null) {
            newPortfolio.getTransactions().add(this);
        }
    }
}
