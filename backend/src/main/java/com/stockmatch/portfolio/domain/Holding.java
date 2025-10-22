package com.stockmatch.portfolio.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.securityasset.domain.Security;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Getter
@AllArgsConstructor
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
}
