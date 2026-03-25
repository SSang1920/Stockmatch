package com.stockmatch.portfolio.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name = "portfolio_daily_summary", indexes = {
        @Index(name = "idx_portfolio_date", columnList = "portfolio_id, date")
})
public class PortfolioDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal totalInvested;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal totalValue;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal totalPnl;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal totalRate;
}
