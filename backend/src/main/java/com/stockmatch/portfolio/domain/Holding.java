package com.stockmatch.portfolio.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.stock.domain.Security;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "holding")
public class Holding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holding_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id")
    private Security security;

    @Column(precision = 28, scale = 8)
    private BigDecimal quantity;

    @Column(name = "avg_price", precision = 28, scale = 8)
    private BigDecimal avgPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency = Currency.KRW;

    // === 연관관계 편의 메서드 === //
    public void setPortfolio(Portfolio newPortfolio) {
        if (this.portfolio == newPortfolio) {
            return;
        }

        // 기존 연관관계 끊기
        if (this.portfolio != null) {
            this.portfolio.getHoldings().remove(this);
        }

        // 새 연관관계 설정
        this.portfolio = newPortfolio;
        if (newPortfolio != null) {
            newPortfolio.getHoldings().add(this);
        }
    }
}
