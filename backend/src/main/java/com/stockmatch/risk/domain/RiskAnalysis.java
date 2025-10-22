package com.stockmatch.risk.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.portfolio.domain.Portfolio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "risk_analysis")
public class RiskAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "risk_analysis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(name = "analysis_date")
    private LocalDate analysisDate;

    @Column(precision = 10, scale = 5)
    private BigDecimal beta;

    @Column(precision = 10, scale = 5)
    private BigDecimal standardDeviation;

    @Column(precision = 10, scale = 5)
    private BigDecimal sharpeRatio;

    @Column(precision = 10, scale = 5)
    private BigDecimal maxDrawdown;
}
