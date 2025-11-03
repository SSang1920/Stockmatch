package com.stockmatch.stock.domain;

import com.stockmatch.WatchItem.WatchItem;
import com.stockmatch.common.BaseEntity;
import com.stockmatch.portfolio.domain.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "security")
public class Security extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "security_id")
    private Long id;

    @Column(length = 30, nullable = false)
    private String symbol;      // 종목 코드

    @Column(length = 100)
    private String name;        // 종목명

    @Enumerated(EnumType.STRING)
    private Market market;      // 시장 구분 (KR, US)

    @Enumerated(EnumType.STRING)
    private Exchange exchange;  // 거래소

    @Enumerated(EnumType.STRING)
    private Currency currency;  // 거래 통화

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SecurityType type;

    @Column(length = 20)
    private String figi;

    @Column(length = 20)
    private String isin;

    @Column
    private boolean delisted = false;       // 상장폐지 여부

    @OneToMany(mappedBy = "security")
    private List<DailyPrice> dailyPrices = new ArrayList<>();

    @OneToOne(mappedBy = "security", fetch = FetchType.LAZY)
    private WatchItem watchItem;

    // ===== 헬퍼 메서드 ====

    /**
     * 국내 종목 여부 판별
     */
    public boolean isKorean() {
        return market == Market.KOR
                || exchange == Exchange.KOSPI
                || exchange == Exchange.KOSDAQ;
    }

    /**
     * 해외 종목 여부 판별
     */
    public boolean isForeign() {
        return !isKorean();
    }

    /**
     * 거래 통화 반환 (기본값: KRW)
     */
    public Currency getCurrencyCode() {
        return currency != null ? currency : Currency.KRW;
    }
}
