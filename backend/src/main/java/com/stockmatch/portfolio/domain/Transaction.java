package com.stockmatch.portfolio.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.stock.domain.Security;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "transaction")
@Builder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;    // 거래가 속한 포트폴리오

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;      // 거래한 종목

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;         // 거래 종류 (매수/매도)

    @Column(precision = 28, scale = 8, nullable = false)
    private BigDecimal quantity;    // 거래 수량

    @Column(precision = 28, scale = 8, nullable = false)
    private BigDecimal price;       // 거래 가격

    @Column(precision = 28, scale = 8, nullable = false)
    private BigDecimal fee;         // 수수료 (없으면 0으로 저장)

    @Column(name = "trade_at", nullable = false)
    private LocalDateTime tradeAt;  // 거래 일시

    @Column(length = 255)
    private String memo;            // 메모

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

    public boolean isInitialBuy() {
        return this.type == TradeType.INITIAL_BUY;
    }
}
