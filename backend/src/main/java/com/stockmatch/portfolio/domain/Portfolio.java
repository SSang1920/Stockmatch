package com.stockmatch.portfolio.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.risk.domain.RiskAnalysis;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@Table(name = "portfolio")
public class Portfolio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false)
    private Currency baseCurrency = Currency.KRW;

    @OneToMany(mappedBy = "portfolio")
    private List<Holding> holdings = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio")
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio")
    private List<RiskAnalysis> riskAnalyses = new ArrayList<>();
}
