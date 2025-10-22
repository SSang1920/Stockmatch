package com.stockmatch.exchangeRate;

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
@Table(name = "exchange_rate")
public class ExchangeRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_rate_id")
    private Long id;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private FromCurrency fromCurrency;

    @Enumerated(EnumType.STRING)
    private ToCurrency toCurrency;

    @Column(precision = 20, scale = 8, nullable = false)
    private BigDecimal rate;
}
