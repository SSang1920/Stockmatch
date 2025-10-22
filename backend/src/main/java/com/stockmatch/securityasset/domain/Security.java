package com.stockmatch.securityasset.domain;

import com.stockmatch.WatchItem.WatchItem;
import com.stockmatch.common.BaseEntity;
import com.stockmatch.portfolio.domain.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@Table(name = "security")
public class Security extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "security_id")
    private Long id;

    @Column(length = 30, nullable = false)
    private String ticker;      // 종목 코드

    @Column(length = 100)
    private String name;        // 종목 공식 명칭

    @Enumerated(EnumType.STRING)
    private Market market;

    @Enumerated(EnumType.STRING)
    private Exchange exchange;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SecurityType type;

    @Column(length = 20)
    private String figi;

    @Column(length = 20)
    private String isin;

    @Column
    private boolean delisted;

    @OneToMany(mappedBy = "security")
    private List<DailyPrice> dailyPrices = new ArrayList<>();

    @OneToOne
    private WatchItem watchItem;
}
