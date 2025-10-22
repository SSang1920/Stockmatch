package com.stockmatch.securityasset.domain;

import com.stockmatch.common.BaseEntity;
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
@Table(name = "daily_price")
public class DailyPrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_price_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id")
    private Security security;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "open_price", precision = 28, scale = 8)
    private BigDecimal openPrice;

    @Column(name = "close_price", precision = 28, scale = 8)
    private BigDecimal closePrice;

    @Column(name = "high_price", precision = 28, scale = 8)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 28, scale = 8)
    private BigDecimal lowPrice;

    @Column(precision = 20, scale = 0)
    private BigDecimal volume;

}
