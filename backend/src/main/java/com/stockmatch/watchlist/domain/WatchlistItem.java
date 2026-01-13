package com.stockmatch.watchlist.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.stock.domain.Security;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "watchlist_item")
public class WatchlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watchlist_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private Watchlist watchlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;

    private String memo;

    @Column(name = "order_no")
    private Integer orderNo;

    // ===== 편의 메서드 =====
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    // ===== 연관관계 편의 메서드 =====
    protected void setWatchlist(Watchlist watchlist) {
        this.watchlist = watchlist;
    }
}
